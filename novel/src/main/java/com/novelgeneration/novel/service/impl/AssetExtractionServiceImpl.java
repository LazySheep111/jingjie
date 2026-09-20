package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.dto.AssetExtractRequest;
import com.novelgeneration.novel.mapper.ChapterMapper;
import com.novelgeneration.novel.mapper.StoryboardMapper;
import com.novelgeneration.novel.mapper.VisualAssetMapper;
import com.novelgeneration.novel.service.AssetExtractionService;
import com.novelgeneration.novel.service.VisualAssetService;
import com.novelgeneration.novel.service.VisualStyleService;
import com.novelgeneration.novel.utils.AiUtil;
import com.novelgeneration.novel.utils.StructuredOutputSchemas;
import com.novelgeneration.novel.utils.StructuredOutputValidator;
import com.novelgeneration.novel.vo.AssetTaskVO;
import com.novelgeneration.novel.vo.ChapterVO;
import com.novelgeneration.novel.vo.StoryboardVO;
import com.novelgeneration.novel.vo.VisualAssetVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class AssetExtractionServiceImpl implements AssetExtractionService {

    @Resource
    private VisualAssetMapper visualAssetMapper;

    @Resource
    private VisualAssetService visualAssetService;

    @Resource
    private VisualStyleService visualStyleService;

    @Resource
    private ChapterMapper chapterMapper;

    @Resource
    private StoryboardMapper storyboardMapper;

    @Resource
    private AiUtil aiUtil;

    @Resource
    ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @Value("${asset.extract.processing-timeout-ms:600000}")
    private long processingTimeoutMs = 600000L;

    @Override
    public AssetTaskVO start(Long novelId, Long chapterNum, AssetExtractRequest request) {
        requireChapter(novelId, chapterNum);
        AssetTaskVO running = visualAssetMapper.selectRunningTask(novelId, chapterNum);
        if (running != null) {
            if ("PENDING".equals(running.getStatus())) {
                executor.submit(() -> execute(running.getTaskId()));
            } else if ("PROCESSING".equals(running.getStatus()) && isStale(running)) {
                int timeoutSeconds = (int) Math.max(1, processingTimeoutMs / 1000);
                if (visualAssetMapper.resetStaleTask(running.getTaskId(), timeoutSeconds) > 0) {
                    executor.submit(() -> execute(running.getTaskId()));
                }
            }
            return running;
        }
        AssetTaskVO task = new AssetTaskVO();
        task.setTaskId(UUID.randomUUID().toString());
        task.setNovelId(novelId);
        task.setChapterNum(chapterNum);
        task.setStatus("PENDING");
        task.setTotal(0);
        task.setReused(0);
        task.setCreated(0);
        task.setFailed(0);
        task.setMessage("正在提交资产提取任务");
        visualAssetMapper.insertTask(task);
        executor.submit(() -> execute(task.getTaskId()));
        return task;
    }

    @Override
    public AssetTaskVO getStatus(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId 不能为空");
        }
        AssetTaskVO task = visualAssetMapper.selectTask(taskId);
        if (task == null) {
            throw new IllegalArgumentException("资产提取任务不存在");
        }
        return task;
    }

    @Override
    @Transactional
    public void execute(String taskId) {
        if (visualAssetMapper.claimTask(taskId) == 0) {
            return;
        }
        AssetTaskVO task = null;
        try {
            task = getStatus(taskId);
            task.setMessage("正在识别当前章节的人物和场景");
            visualAssetMapper.updateTask(task);
            ChapterVO chapter = chapterMapper.selectChapterByNum(task.getNovelId(), task.getChapterNum());
            if (chapter == null || chapter.getChapterText() == null || chapter.getChapterText().isBlank()) {
                throw new IllegalArgumentException("当前章节没有正文");
            }
            String storyboardText = storyboardText(task.getNovelId(), task.getChapterNum());
            task.setMessage("正在分析章节中的人物和场景");
            visualAssetMapper.updateTask(task);
            String entityResponse = aiUtil.chatJsonObject(buildEntityPrompt(chapter, storyboardText));
            List<VisualAssetVO> entities;
            try {
                entities = parseEntities(entityResponse);
            } catch (IllegalArgumentException parseError) {
                log.warn("Entity JSON parse failed, asking AI to repair taskId={}", taskId);
                entities = parseEntities(aiUtil.chatJsonObject(buildEntityRepairPrompt(entityResponse)));
            }
            task.setTotal(entities.size());
            visualAssetMapper.updateTask(task);
            int entityIndex = 0;
            for (VisualAssetVO entity : entities) {
                try {
                    entity.setNovelId(task.getNovelId());
                    entityIndex++;
                    task.setMessage("正在生成第 " + entityIndex + " / " + entities.size() + " 个资产的三视图提示词：" + entity.getAssetName());
                    visualAssetMapper.updateTask(task);
                    VisualAssetVO asset = visualAssetService.findOrCreate(
                            task.getNovelId(), entity.getAssetType(), entity.getAssetName(),
                            entity.getNormalizedName(), entity.getCoreFeatures());
                    if ("PROMPT_PENDING".equals(asset.getStatus())) {
                        VisualAssetVO prompts = generatePrompts(entity);
                        asset.setCoreFeatures(entity.getCoreFeatures());
                        asset.setFrontPrompt(prompts.getFrontPrompt());
                        asset.setSidePrompt(prompts.getSidePrompt());
                        asset.setBackPrompt(prompts.getBackPrompt());
                        asset.setStatus("PROMPT_READY");
                        visualAssetService.update(task.getNovelId(), asset.getAssetId(), asset);
                        task.setCreated(task.getCreated() + 1);
                    } else {
                        task.setReused(task.getReused() + 1);
                    }
                    attachAssetToScenes(task.getNovelId(), task.getChapterNum(), asset);
                } catch (Exception entityError) {
                    task.setFailed(task.getFailed() + 1);
                    log.warn("Extract visual asset failed: novelId={}, chapterNum={}, asset={}",
                            task.getNovelId(), task.getChapterNum(), entity.getAssetName(), entityError);
                }
                visualAssetMapper.updateTask(task);
            }
            task.setStatus(task.getFailed() > 0 ? "PARTIAL_FAILED" : "COMPLETED");
            task.setMessage(task.getFailed() > 0 ? "部分资产处理失败" : "资产提取完成");
            visualAssetMapper.updateTask(task);
        } catch (Exception e) {
            if (task == null) {
                try {
                    task = getStatus(taskId);
                } catch (Exception statusError) {
                    log.error("Cannot reload failed asset extraction task: taskId={}", taskId, statusError);
                }
            }
            if (task != null) {
                task.setStatus("FAILED");
                task.setErrorMessage(e.getMessage());
                task.setMessage("资产提取失败");
                try {
                    visualAssetMapper.updateTask(task);
                } catch (Exception updateError) {
                    log.error("Cannot mark asset extraction task failed: taskId={}", taskId, updateError);
                }
            }
            log.error("Asset extraction failed: taskId={}", taskId, e);
        }
    }

    private boolean isStale(AssetTaskVO task) {
        return task.getUpdateTime() == null
                || java.time.Duration.between(task.getUpdateTime(), java.time.LocalDateTime.now()).toMillis() >= processingTimeoutMs;
    }

    @Override
    public List<VisualAssetVO> parseEntities(String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(aiResponse));
            StructuredOutputValidator.validate(root, StructuredOutputSchemas.assetEntities(objectMapper));
            JsonNode entitiesNode = root.get("entities");
            if (entitiesNode == null || !entitiesNode.isArray() || entitiesNode.isEmpty()) {
                throw new IllegalArgumentException("AI 未返回有效资产列表");
            }
            List<VisualAssetVO> entities = new ArrayList<>();
            for (JsonNode entityNode : entitiesNode) {
                VisualAssetVO entity = new VisualAssetVO();
                entity.setAssetType(entityNode.path("assetType").asText(null));
                entity.setAssetName(entityNode.path("assetName").asText(null));
                entity.setCoreFeatures(entityNode.path("coreFeatures").asText(""));
                entity.setNormalizedName(normalize(entity.getAssetName()));
                if (!"CHARACTER".equals(entity.getAssetType()) && !"LOCATION".equals(entity.getAssetType())) {
                    throw new IllegalArgumentException("资产类型必须是 CHARACTER 或 LOCATION");
                }
                if (entity.getAssetName() == null || entity.getAssetName().isBlank()) {
                    throw new IllegalArgumentException("资产名称不能为空");
                }
                entities.add(entity);
            }
            return entities;
        } catch (Exception e) {
            throw new IllegalArgumentException("AI 返回的资产格式不正确", e);
        }
    }

    @Override
    public VisualAssetVO generatePrompts(VisualAssetVO entity) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(aiUtil.chatJsonObject(buildPromptPrompt(entity))));
            StructuredOutputValidator.validate(root, StructuredOutputSchemas.assetPrompts(objectMapper));
            VisualAssetVO prompts = new VisualAssetVO();
            prompts.setFrontPrompt(root.path("frontPrompt").asText(null));
            prompts.setSidePrompt(root.path("sidePrompt").asText(null));
            prompts.setBackPrompt(root.path("backPrompt").asText(null));
            if (isBlank(prompts.getFrontPrompt()) || isBlank(prompts.getSidePrompt()) || isBlank(prompts.getBackPrompt())) {
                throw new IllegalArgumentException("AI 未返回完整的三视图提示词");
            }
            return prompts;
        } catch (Exception e) {
            throw new IllegalArgumentException("AI 返回的三视图提示词格式不正确", e);
        }
    }

    private void attachAssetToScenes(Long novelId, Long chapterNum, VisualAssetVO asset) {
        StoryboardVO storyboard = storyboardMapper.selectLatest(novelId, chapterNum);
        if (storyboard == null) {
            return;
        }
        List<StoryboardVO.Scene> scenes = storyboardMapper.selectScenes(storyboard.getId());
        String assetName = asset.getAssetName();
        for (StoryboardVO.Scene scene : scenes) {
            String searchable = String.join(" ", value(scene.getCharacters()), value(scene.getLocation()),
                    value(scene.getShotPlan()));
            if (searchable.contains(assetName)) {
                visualAssetMapper.insertSceneAssetRef(scene.getId(), asset.getAssetId(), asset.getVersion(), asset.getAssetType());
            }
        }
    }

    private String storyboardText(Long novelId, Long chapterNum) {
        StoryboardVO storyboard = storyboardMapper.selectLatest(novelId, chapterNum);
        if (storyboard == null) {
            return "当前章节暂无分镜脚本";
        }
        StringBuilder text = new StringBuilder();
        for (StoryboardVO.Scene scene : storyboardMapper.selectScenes(storyboard.getId())) {
            text.append("场景：").append(value(scene.getLocation()))
                    .append("；人物：").append(value(scene.getCharacters()))
                    .append("；镜头脚本：").append(value(scene.getShotPlan())).append("\n");
        }
        return text.toString();
    }

    private String buildEntityPrompt(ChapterVO chapter, String storyboardText) {
        return "请从以下小说章节正文和分镜脚本中提取需要保持视觉一致的人物和场景。"
                + "只返回严格合法 JSON，不要 Markdown，不要解释。人物类型使用 CHARACTER，场景类型使用 LOCATION。"
                + "同一个实体只返回一次，最多返回 12 个实体，核心特征控制在 80 个中文字符以内。"
                + "名称使用稳定、明确的标准名称。格式："
                + "{\"entities\":[{\"assetType\":\"CHARACTER\",\"assetName\":\"名称\",\"coreFeatures\":\"核心外观和设定\"}]}"
                + "\n" + visualStyleService.buildPrompt(chapter.getNovelId())
                + "\n章节标题：" + value(chapter.getChapterTitle())
                + "\n章节正文：\n" + value(chapter.getChapterText())
                + "\n分镜脚本：\n" + storyboardText;
    }

    private String buildEntityRepairPrompt(String rawResponse) {
        String response = rawResponse == null ? "" : rawResponse;
        if (response.length() > 8000) {
            response = response.substring(0, 8000);
        }
        return "上一次 AI 返回的资产 JSON 不完整或格式错误。请根据下面内容修复并补全，"
                + "只返回严格合法 JSON，不要 Markdown，不要解释。"
                + "entities 必须是数组，每个对象只包含 assetType、assetName、coreFeatures；"
                + "assetType 只能是 CHARACTER 或 LOCATION，coreFeatures 使用中文且不超过 80 个字符。"
                + "如果原内容被截断，请根据已有信息合理补全数组并闭合所有括号。"
                + "\n待修复内容：\n" + response;
    }

    private String buildPromptPrompt(VisualAssetVO entity) {
        return "请为以下小说视觉资产生成三视图绘图提示词。只返回严格合法 JSON，不要 Markdown，不要解释。"
                + "必须包含 frontPrompt、sidePrompt、backPrompt 三个字段，三个字段的内容必须全部使用中文，禁止输出英文描述；JSON 字段名仍保持英文。"
                + "生成结果必须严格遵守下面的小说统一视觉设定，不能擅自改成现代、未来或其他时代风格。\n"
                + visualStyleService.buildPrompt(entity.getNovelId())
                + "资产类型：" + value(entity.getAssetType())
                + "；资产名称：" + value(entity.getAssetName())
                + "；核心特征：" + value(entity.getCoreFeatures());
    }

    private String extractJson(String response) {
        if (response == null) {
            throw new IllegalArgumentException("AI response is null");
        }
        String content = response.trim();
        if (content.startsWith("```")) {
            content = content.replaceFirst("^```(?:json)?\\s*", "");
            content = content.replaceFirst("\\s*```$", "");
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("AI response is not a JSON object");
        }
        return content.substring(start, end + 1);
    }

    private void requireChapter(Long novelId, Long chapterNum) {
        if (novelId == null || novelId < 1 || chapterNum == null || chapterNum < 1) {
            throw new IllegalArgumentException("novelId 和 chapterNum 必须大于 0");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "").toLowerCase();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
