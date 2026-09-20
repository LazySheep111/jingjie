package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.dto  .StoryboardGenerateRequest;
import com.novelgeneration.novel.mapper.StoryboardMapper;
import com.novelgeneration.novel.service.StoryboardService;
import com.novelgeneration.novel.service.VisualStyleService;
import com.novelgeneration.novel.utils.AiUtil;
import com.novelgeneration.novel.utils.StructuredOutputSchemas;
import com.novelgeneration.novel.utils.StructuredOutputValidator;
import com.novelgeneration.novel.vo.StoryboardVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class StoryboardServiceImpl implements StoryboardService {

    private static final Pattern TOTAL_TIME_PATTERN = Pattern.compile("(?m)^\\s*时长\\s+(\\d+):(\\d{2})\\s*-\\s*(\\d+):(\\d{2})\\s*$");
    private static final Pattern SHOT_TIME_PATTERN = Pattern.compile("\\[(\\d+):(\\d{2})\\s*-\\s*(\\d+):(\\d{2})\\s*\\|([^]]+)]");

    @Resource
    private StoryboardMapper storyboardMapper;

    @Resource
    private AiUtil aiUtil;

    @Resource
    private VisualStyleService visualStyleService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public StoryboardVO generate(Long novelId, StoryboardGenerateRequest request) {
        if (novelId == null || request == null || request.getChapterNum() == null) {
            throw new IllegalArgumentException("novelId、chapterNum 和请求内容不能为空");
        }
        if (request.getChapterText() == null || request.getChapterText().isBlank()) {
            throw new IllegalArgumentException("当前章节没有正文，无法生成分镜脚本");
        }

        String response = aiUtil.chatJsonObject(buildPrompt(request));
        StoryboardVO generated = parseResponse(response);
        generated.setNovelId(novelId);
        generated.setChapterNum(request.getChapterNum());
        generated.setVersion(storyboardMapper.nextVersion(novelId, request.getChapterNum()));
        generated.setStatus("COMPLETED");
        if (generated.getTotalDurationSec() == null) {
            generated.setTotalDurationSec(generated.getScenes().stream()
                    .map(StoryboardVO.Scene::getDurationSec)
                    .filter(value -> value != null)
                    .mapToInt(Integer::intValue)
                    .sum());
        }

        storyboardMapper.insertStoryboard(generated);
        for (StoryboardVO.Scene scene : generated.getScenes()) {
            storyboardMapper.insertScene(generated.getId(), request.getChapterNum(), scene);
        }
        return generated;
    }

    @Override
    public StoryboardVO getLatest(Long novelId, Long chapterNum) {
        StoryboardVO storyboard = storyboardMapper.selectLatest(novelId, chapterNum);
        if (storyboard == null) {
            return null;
        }
        storyboard.setScenes(storyboardMapper.selectScenes(storyboard.getId()));
        for (StoryboardVO.Scene scene : storyboard.getScenes()) {
            scene.setCharacterAssetIds(storyboardMapper.selectAssetIds(scene.getId(), "CHARACTER"));
            scene.setLocationAssetIds(storyboardMapper.selectAssetIds(scene.getId(), "LOCATION"));
        }
        return storyboard;
    }

    @Override
    public StoryboardVO update(Long novelId, Long chapterNum, StoryboardVO storyboard) {
        if (storyboard == null || storyboard.getScenes() == null) {
            throw new IllegalArgumentException("分镜内容不能为空");
        }
        StoryboardVO saved = getLatest(novelId, chapterNum);
        if (saved == null) {
            throw new IllegalArgumentException("当前章节还没有分镜脚本");
        }
        int totalDurationSec = 0;
        for (StoryboardVO.Scene scene : storyboard.getScenes()) {
            if (scene.getId() == null) {
                throw new IllegalArgumentException("分镜缺少 sceneId");
            }
            int durationSec = validateShotPlan(scene.getShotPlan());
            scene.setDurationSec(durationSec);
            totalDurationSec += durationSec;
            storyboardMapper.updateScene(scene);
        }
        storyboardMapper.updateTotalDuration(saved.getId(), totalDurationSec);
        saved.setTotalDurationSec(totalDurationSec);
        return getLatest(novelId, chapterNum);
    }

    @Override
    public void removeAssetReference(Long novelId, Long chapterNum, Long sceneId, Long assetId, String assetRole) {
        requirePositiveId(novelId, "novelId");
        requirePositiveId(chapterNum, "chapterNum");
        requirePositiveId(sceneId, "sceneId");
        requirePositiveId(assetId, "assetId");
        if (!List.of("CHARACTER", "LOCATION").contains(assetRole)) {
            throw new IllegalArgumentException("资产角色只能是 CHARACTER 或 LOCATION");
        }
        if (storyboardMapper.deleteAssetReference(novelId, chapterNum, sceneId, assetId, assetRole) == 0) {
            throw new IllegalArgumentException("当前分镜未关联该参考资产");
        }
    }

    @Override
    public String exportText(Long novelId, Long chapterNum) {
        StoryboardVO storyboard = getLatest(novelId, chapterNum);
        if (storyboard == null) {
            throw new IllegalArgumentException("当前章节还没有分镜脚本");
        }
        StringBuilder text = new StringBuilder();
        text.append("第 ").append(chapterNum).append(" 章分镜脚本\n\n");
        for (StoryboardVO.Scene scene : storyboard.getScenes()) {
            text.append("分镜 ").append(scene.getSequence()).append("\n")
                    .append(value(scene.getShotPlan())).append("\n")
                    .append("场景：").append(value(scene.getLocation())).append("\n\n");
        }
        return text.toString();
    }

    private String formatTime(int seconds) {
        int safeSeconds = Math.max(0, seconds);
        return (safeSeconds / 60) + ":" + String.format("%02d", safeSeconds % 60);
    }

    private StoryboardVO parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(response));
            JsonNode storyboardNode = root.has("storyboard") ? root.get("storyboard") : root;
            StructuredOutputValidator.validate(storyboardNode, StructuredOutputSchemas.storyboard(objectMapper));
            StoryboardVO result = new StoryboardVO();
            if (storyboardNode.has("totalDurationSec")) {
                result.setTotalDurationSec(storyboardNode.get("totalDurationSec").asInt());
            }
            JsonNode scenesNode = storyboardNode.get("scenes");
            if (scenesNode == null || !scenesNode.isArray() || scenesNode.isEmpty()) {
                throw new IllegalArgumentException("AI 未返回有效分镜列表");
            }
            for (JsonNode sceneNode : scenesNode) {
                StoryboardVO.Scene scene = objectMapper.treeToValue(sceneNode, StoryboardVO.Scene.class);
                if (scene.getSequence() == null) {
                    scene.setSequence(result.getScenes().size() + 1);
                }
                scene.setDurationSec(validateShotPlan(scene.getShotPlan()));
                result.getScenes().add(scene);
            }
            result.setTotalDurationSec(result.getScenes().stream()
                    .map(StoryboardVO.Scene::getDurationSec)
                    .mapToInt(Integer::intValue)
                    .sum());
            return result;
        } catch (Exception e) {
            log.error("Parse AI storyboard response failed: {}", response, e);
            throw new IllegalArgumentException("AI 返回的分镜格式不正确", e);
        }
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

    private String buildPrompt(StoryboardGenerateRequest request) {
        return "请根据以下小说章节正文生成影视分镜脚本。只返回严格合法的 JSON，不要 Markdown，不要解释文字。"
                + "每个分镜必须包含一个 shotPlan 多行文本和连续时间轴，内容丰富时可以包含多个连续镜头，内容简单时可以只有一个镜头；每个分镜最多 6 个镜头。"
                + "每个分镜总时长默认 3-8 秒，最长 10 秒；台词过多时优先保留完整台词，必要时延长到 10 秒，仍然放不下就拆成连续分镜。"
                + "shotPlan 第一行必须严格使用：时长 0:00-0:05。后续每个镜头必须严格使用：[0:00-0:02 | 远景 -> 全景]：画面描述。"
                + "镜头所属人物的台词、音效和背景音乐必须紧跟在该镜头下面，每句台词单独一行，例如：台词（林晓晓，声音发颤 / 语速极快）：冉学姐！救命！；音效：大门撞击声；背景音乐：紧张弦乐。"
                + "镜头时间必须从 0:00 开始，连续递进，不能重叠、不能留空档，最后一个镜头必须正好结束于总时长。不得改变原剧情，不得凭空增加主要事件，保持人物、服装、道具和场景连续。"
                + "JSON 格式必须为：{\"totalDurationSec\":数字,\"scenes\":[{"
                + "\"sequence\":数字,\"durationSec\":数字,\"location\":字符串,"
                + "\"timeOfDay\":字符串,\"weather\":字符串,\"characters\":字符串,"
                + "\"shotType\":字符串,\"cameraMovement\":字符串,"
                + "\"shotPlan\":字符串,\"characterEmotion\":字符串,"
                + "\"voiceOver\":字符串,\"transition\":字符串,"
                + "\"imagePrompt\":字符串}]}。"
                + "画面风格：" + value(request.getVisualStyle()) + "；画面比例：" + value(request.getAspectRatio()) + "。"
                + "以下是本部小说的统一视觉设定，所有分镜的 imagePrompt 和 shotPlan 都必须遵守，不能出现与其冲突的时代、建筑、材质、色彩或画风：\n"
                + visualStyleService.buildPrompt(request.getNovelId())
                + "章节标题：" + value(request.getChapterTitle()) + "。\n章节正文：\n" + request.getChapterText();
    }

    private String value(String text) {
        return text == null ? "" : text;
    }

    private int validateShotPlan(String shotPlan) {
        if (shotPlan == null || shotPlan.isBlank()) {
            throw new IllegalArgumentException("镜头脚本不能为空");
        }
        Matcher totalMatcher = TOTAL_TIME_PATTERN.matcher(shotPlan);
        if (!totalMatcher.find()) {
            throw new IllegalArgumentException("镜头脚本第一行必须使用“时长 0:00-0:05”格式");
        }
        int totalStart = toSeconds(totalMatcher.group(1), totalMatcher.group(2));
        int totalEnd = toSeconds(totalMatcher.group(3), totalMatcher.group(4));
        int totalDuration = totalEnd - totalStart;
        if (totalStart != 0 || totalDuration < 3 || totalDuration > 10) {
            throw new IllegalArgumentException("分镜总时长必须从 0:00 开始，且在 3 到 10 秒之间");
        }

        Matcher shotMatcher = SHOT_TIME_PATTERN.matcher(shotPlan);
        int count = 0;
        int expectedStart = 0;
        while (shotMatcher.find()) {
            int start = toSeconds(shotMatcher.group(1), shotMatcher.group(2));
            int end = toSeconds(shotMatcher.group(3), shotMatcher.group(4));
            if (start != expectedStart || end <= start) {
                throw new IllegalArgumentException("镜头时间轴必须连续且不能重叠");
            }
            expectedStart = end;
            count++;
        }
        if (count < 1 || count > 6 || expectedStart != totalEnd) {
            throw new IllegalArgumentException("镜头时间轴必须包含 1 到 6 个镜头，并完整覆盖总时长");
        }
        return totalDuration;
    }

    private int toSeconds(String minute, String second) {
        return Integer.parseInt(minute) * 60 + Integer.parseInt(second);
    }

    private void requirePositiveId(Long value, String name) {
        if (value == null || value < 1) {
            throw new IllegalArgumentException(name + " 必须大于 0");
        }
    }
}
