package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.dto.OutlineDTO;
import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.mapper.NovelTextMapper;
import com.novelgeneration.novel.service.GenerateFullService;
import com.novelgeneration.novel.utils.AiUtil;
import com.novelgeneration.novel.utils.StructuredOutputSchemas;
import com.novelgeneration.novel.utils.StructuredOutputValidator;
import com.novelgeneration.novel.vo.NovelVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GenerateFullServiceImpl implements GenerateFullService {

    private static final Map<Long, GenerationState> GENERATION_STATE = new ConcurrentHashMap<>();

    @Resource
    private NovelTextMapper novelTextMapper;

    @Resource
    private AiUtil aiUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Result GenerateFull(Long novelId, OutlineDTO outlineDTO) {
        outlineDTO.setNovelId(novelId);
        if (outlineDTO.getOutline() == null || outlineDTO.getOutline().getChapterList() == null || outlineDTO.getOutline().getChapterList().isEmpty()) {
            throw new RuntimeException("Outline chapterList is empty");
        }

        List<NovelVO.NovelText> existingChapters = novelTextMapper.selectNovelText(novelId);
        int existingChapterCount = existingChapters == null ? 0 : existingChapters.size();
        GENERATION_STATE.put(novelId, GenerationState.generating(existingChapterCount));

        LocalDateTime createTime = LocalDateTime.now();
        for (OutlineDTO.ChapterItem chapterOutline : outlineDTO.getOutline().getChapterList()) {
            if (chapterOutline.getChapterNum() != null && chapterOutline.getChapterNum() <= existingChapterCount) {
                continue;
            }

            try {
                String prompt = buildChapterPrompt(outlineDTO, chapterOutline);
                log.info("生成第{}章小说的提示词:{}", chapterOutline.getChapterNum(), prompt);
                String aiResponse = aiUtil.chatJsonObject(prompt);
                log.info("第{}章小说正文:{}", chapterOutline.getChapterNum(), aiResponse);

                NovelVO.NovelText chapter = parseChapterWithRepair(aiResponse, outlineDTO, chapterOutline);
                fillChapterFallback(chapter, chapterOutline);
                novelTextMapper.insertChapter(novelId, chapter, createTime, LocalDateTime.now());
                GENERATION_STATE.put(novelId, GenerationState.generating(chapter.getChapterNum()));
            } catch (Exception e) {
                Long failedChapter = chapterOutline.getChapterNum() == null ? null : chapterOutline.getChapterNum().longValue();
                markGenerationFailed(novelId, failedChapter, e.getMessage());
                throw e;
            }
        }

        GENERATION_STATE.remove(novelId);
        Map<String, Object> data = new HashMap<>();
        data.put("novelId", novelId);
        data.put("status", "FULL_GENERATED");
        return Result.ok(data);
    }

    public static GenerationState getGenerationState(Long novelId) {
        return GENERATION_STATE.getOrDefault(novelId, GenerationState.none());
    }

    public static void markGenerationFailed(Long novelId, Long currentChapter, String errorMessage) {
        GENERATION_STATE.put(novelId, GenerationState.failed(currentChapter, errorMessage));
    }

    private NovelVO.NovelText parseChapterWithRepair(String aiResponse, OutlineDTO outlineDTO, OutlineDTO.ChapterItem chapterOutline) {
        try {
            return parseChapter(aiResponse);
        } catch (RuntimeException firstError) {
            String repairPrompt = buildRepairPrompt(outlineDTO, chapterOutline, aiResponse, firstError.getMessage());
            log.warn("第{}章小说正文 JSON 解析失败，正在请求 AI 修正格式: {}", chapterOutline.getChapterNum(), firstError.getMessage());
            String repairedResponse = aiUtil.chatJsonObject(repairPrompt);
            log.info("第{}章小说正文修正后返回:{}", chapterOutline.getChapterNum(), repairedResponse);
            return parseChapter(repairedResponse);
        }
    }

    private NovelVO.NovelText parseChapter(String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(aiResponse));
            JsonNode chapterNode = root;
            if (root.has("chapter")) {
                chapterNode = root.get("chapter");
            } else if (root.has("novelText") && root.get("novelText").isArray() && root.get("novelText").size() > 0) {
                chapterNode = root.get("novelText").get(0);
            } else if (root.has("chapterTextList") && root.get("chapterTextList").isArray() && root.get("chapterTextList").size() > 0) {
                chapterNode = root.get("chapterTextList").get(0);
            }
            StructuredOutputValidator.validate(chapterNode, StructuredOutputSchemas.chapter(objectMapper));
            return objectMapper.treeToValue(chapterNode, NovelVO.NovelText.class);
        } catch (Exception e) {
            log.error("Parse AI chapter response failed, response: {}", aiResponse, e);
            throw new RuntimeException("Parse novel chapter failed", e);
        }
    }

    private void fillChapterFallback(NovelVO.NovelText chapter, OutlineDTO.ChapterItem chapterOutline) {
        if (chapter.getChapterNum() == null) {
            chapter.setChapterNum(chapterOutline.getChapterNum());
        }
        if (chapter.getChapterTitle() == null || chapter.getChapterTitle().isBlank()) {
            chapter.setChapterTitle(chapterOutline.getChapterTitle());
        }
        if (chapter.getChapterSummary() == null || chapter.getChapterSummary().isBlank()) {
            chapter.setChapterSummary(chapterOutline.getChapterSummary());
        }
    }

    private String extractJson(String aiResponse) {
        if (aiResponse == null) {
            throw new IllegalArgumentException("AI response is null");
        }

        String content = aiResponse.trim();
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

    private String buildChapterPrompt(OutlineDTO outlineDTO, OutlineDTO.ChapterItem chapter) {
        return "根据以下小说大纲，只生成指定章节的完整小说正文。仅返回一个有效 JSON 对象，不要包含解释、Markdown 或代码块。\n" +
                "JSON 对象必须包含且只能包含：chapterNum、chapterTitle、chapterSummary、chapterText。\n" +
                "小说标题: " + outlineDTO.getOutline().getNovelTitle() + "\n" +
                "整体梗概: " + outlineDTO.getOutline().getOverallPlot() + "\n" +
                "当前章节编号: " + chapter.getChapterNum() + "\n" +
                "当前章节标题: " + chapter.getChapterTitle() + "\n" +
                "当前章节概要: " + chapter.getChapterSummary() + "\n" +
                "建议字数: " + chapter.getWordCount() + "\n";
    }

    private String buildRepairPrompt(OutlineDTO outlineDTO, OutlineDTO.ChapterItem chapter, String invalidResponse, String errorMessage) {
        return "你上一次返回的小说章节内容不是合法 JSON，后端解析失败。\n" +
                "错误原因: " + errorMessage + "\n" +
                "请把上一次返回内容修正为严格合法的 JSON 对象，只返回 JSON，不要 Markdown，不要解释文字。\n" +
                "JSON 对象必须包含且只能包含这些字段: chapterNum, chapterTitle, chapterSummary, chapterText。\n" +
                "字段要求:\n" +
                "- chapterNum 必须是数字: " + chapter.getChapterNum() + "\n" +
                "- chapterTitle 必须是字符串: " + chapter.getChapterTitle() + "\n" +
                "- chapterSummary 必须是字符串，可参考: " + chapter.getChapterSummary() + "\n" +
                "- chapterText 必须是字符串，保留完整正文内容\n" +
                "小说标题: " + outlineDTO.getOutline().getNovelTitle() + "\n" +
                "整体梗概: " + outlineDTO.getOutline().getOverallPlot() + "\n" +
                "上一次返回内容如下:\n" +
                invalidResponse;
    }

    public static class GenerationState {
        private final String status;
        private final Long currentChapter;
        private final String errorMessage;

        private GenerationState(String status, Long currentChapter, String errorMessage) {
            this.status = status;
            this.currentChapter = currentChapter;
            this.errorMessage = errorMessage;
        }

        public static GenerationState none() {
            return new GenerationState("", null, null);
        }

        public static GenerationState generating(Integer currentChapter) {
            return new GenerationState("GENERATING_FULL", currentChapter == null ? 0L : currentChapter.longValue(), null);
        }

        public static GenerationState failed(Long currentChapter, String errorMessage) {
            return new GenerationState("FAILED", currentChapter, errorMessage);
        }

        public String getStatus() {
            return status;
        }

        public Long getCurrentChapter() {
            return currentChapter;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
