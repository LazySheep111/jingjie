package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.mapper.ChapterMapper;
import com.novelgeneration.novel.mapper.VisualStyleMapper;
import com.novelgeneration.novel.service.VisualStyleService;
import com.novelgeneration.novel.utils.AiUtil;
import com.novelgeneration.novel.vo.ChapterVO;
import com.novelgeneration.novel.vo.VisualStyleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class VisualStyleServiceImpl implements VisualStyleService {
    @Resource
    private VisualStyleMapper visualStyleMapper;

    @Resource
    private ChapterMapper chapterMapper;

    @Resource
    private AiUtil aiUtil;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public VisualStyleVO get(Long novelId) {
        requireNovelId(novelId);
        return visualStyleMapper.selectByNovelId(novelId);
    }

    @Override
    @Transactional
    public VisualStyleVO save(Long novelId, VisualStyleVO request) {
        requireNovelId(novelId);
        if (request == null) {
            throw new IllegalArgumentException("视觉风格配置不能为空");
        }
        VisualStyleVO current = visualStyleMapper.selectByNovelId(novelId);
        VisualStyleVO style = copy(novelId, request);
        if (current == null) {
            visualStyleMapper.insert(style);
        } else {
            visualStyleMapper.update(style);
        }
        return visualStyleMapper.selectByNovelId(novelId);
    }

    @Override
    public VisualStyleVO generateInitial(Long novelId) {
        requireNovelId(novelId);
        List<ChapterVO> chapters = chapterMapper.selectChapter(novelId);
        if (chapters == null || chapters.isEmpty()) {
            throw new IllegalArgumentException("该小说暂无章节内容，无法生成视觉风格");
        }
        String response = aiUtil.chat(buildGenerationPrompt(novelId, chapters));
        try {
            VisualStyleVO style = objectMapper.readValue(extractJson(response), VisualStyleVO.class);
            style.setNovelId(novelId);
            return style;
        } catch (Exception e) {
            throw new IllegalArgumentException("AI 返回的视觉风格格式不正确", e);
        }
    }

    @Override
    public String buildPrompt(Long novelId) {
        VisualStyleVO style = get(novelId);
        if (style == null) {
            return "";
        }
        StringBuilder prompt = new StringBuilder("小说统一视觉设定：\n");
        append(prompt, "时代背景", style.getEra());
        append(prompt, "地域文化", style.getRegion());
        append(prompt, "建筑风格", style.getArchitecture());
        append(prompt, "常用材质", style.getMaterial());
        append(prompt, "色彩风格", style.getColorStyle());
        append(prompt, "光照风格", style.getLightingStyle());
        append(prompt, "整体画风", style.getArtStyle());
        append(prompt, "镜头风格", style.getCameraStyle());
        append(prompt, "补充要求", style.getPositivePrompt());
        append(prompt, "禁止出现", style.getNegativePrompt());
        return prompt.toString();
    }

    private VisualStyleVO copy(Long novelId, VisualStyleVO request) {
        VisualStyleVO style = new VisualStyleVO();
        style.setNovelId(novelId);
        style.setEra(request.getEra());
        style.setRegion(request.getRegion());
        style.setArchitecture(request.getArchitecture());
        style.setMaterial(request.getMaterial());
        style.setColorStyle(request.getColorStyle());
        style.setLightingStyle(request.getLightingStyle());
        style.setArtStyle(request.getArtStyle());
        style.setCameraStyle(request.getCameraStyle());
        style.setPositivePrompt(request.getPositivePrompt());
        style.setNegativePrompt(request.getNegativePrompt());
        return style;
    }

    private String buildGenerationPrompt(Long novelId, List<ChapterVO> chapters) {
        StringBuilder story = new StringBuilder();
        for (ChapterVO chapter : chapters) {
            story.append("第").append(chapter.getChapterNum()).append("章：")
                    .append(value(chapter.getChapterTitle())).append("\n")
                    .append("章节简介：").append(value(chapter.getChapterSummary())).append("\n");
            String text = value(chapter.getChapterText());
            if (text.length() > 500) {
                text = text.substring(0, 500);
            }
            story.append("正文片段：").append(text).append("\n\n");
        }
        return "请根据以下整部小说内容，生成一份统一的全书视觉风格配置。"
                + "重点保证人物、建筑、场景、材质、色彩和光照在整部小说中保持一致。"
                + "只返回严格合法 JSON，不要 Markdown，不要解释。"
                + "字段必须包含：era、region、architecture、material、colorStyle、lightingStyle、"
                + "artStyle、cameraStyle、positivePrompt、negativePrompt。"
                + "每个字段都使用中文，内容具体、可直接用于文生图提示词。\n"
                + "小说已有视觉风格（可能为空）：\n" + buildPrompt(novelId)
                + "小说内容：\n" + story;
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

    private String value(String text) {
        return text == null ? "" : text;
    }

    private void append(StringBuilder prompt, String label, String value) {
        if (value != null && !value.isBlank()) {
            prompt.append(label).append("：").append(value.trim()).append("\n");
        }
    }

    private void requireNovelId(Long novelId) {
        if (novelId == null || novelId < 1) {
            throw new IllegalArgumentException("novelId 必须大于 0");
        }
    }
}
