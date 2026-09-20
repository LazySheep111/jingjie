package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.dto.OutlineDTO;
import com.novelgeneration.novel.entity.NovelInfo;
import com.novelgeneration.novel.mapper.GenerateOutlineMapper;
import com.novelgeneration.novel.service.GenerateOutlineService;
import com.novelgeneration.novel.utils.AiUtil;
import com.novelgeneration.novel.vo.NovelOutlineVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class GenerateOutlineServiceImpl implements GenerateOutlineService {

    @Resource
    private GenerateOutlineMapper generateOutlineMapper;

    @Resource
    private AiUtil aiUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public NovelOutlineVO generateOutline(OutlineDTO outlineDTO) {
        String prompt = buildPrompt(outlineDTO);
        log.info("提示词：{}", prompt);
        String aiResponse;
        try {
            aiResponse = aiUtil.chat(prompt);
            log.info("AI返回：{}", aiResponse);
        } catch (Exception e) {
            log.error("Call AI service failed", e);
            throw new RuntimeException("Call AI service failed", e);
        }

        NovelOutlineVO novelOutlineVO;
        try {
            novelOutlineVO = objectMapper.readValue(extractJson(aiResponse), NovelOutlineVO.class);
        } catch (Exception e) {
            log.error("Parse AI response failed, response: {}", aiResponse, e);
            throw new RuntimeException("Parse outline failed", e);
        }

        NovelInfo novelInfo = convertDtoToEntity(outlineDTO);
        generateOutlineMapper.add(novelInfo);
        novelOutlineVO.setNovelId(novelInfo.getId());

        return novelOutlineVO;
    }

    private String buildPrompt(OutlineDTO dto) {
        return "根据以下用户输入生成小说大纲。仅返回一个有效的 JSON 对象。请勿包含解释、Markdown 或代码块。\n" +
                "JSON 对象必须包含以下字段：novelTitle, totalChapter, overallPlot, foreshadowList, chapterList。\n" +
                "chapterList中的每一项都必须包含：chapterNum、chapterTitle、chapterSummary、wordCount。\n" +
                "用户输入:\n" +
                "小说标题: " + dto.getNovelTitle() + "\n" +
                "小说类型: " + dto.getCategory() + "\n" +
                "小说篇幅: " + dto.getNovelLength() + "\n" +
                "结局类型: " + dto.getEndingType() + "\n" +
                "写在风格: " + dto.getWritingStyle() + "\n" +
                "目标读者: " + dto.getTargetAudience() + "\n" +
                "主角设定: " + dto.getProtagonist() + "\n" +
                "配角设定: " + dto.getRoleList() + "\n" +
                "时代背景: " + dto.getBackground() + "\n" +
                "世界规则: " + dto.getWorldRule() + "\n" +
                "核心主题: " + dto.getTheme() + "\n" +
                "开篇事件: " + dto.getTriggerEvent() + "\n" +
                "伏笔数量: " + dto.getForeshadowCount() + "\n" +
                "叙事视角: " + dto.getNarrativeView() + "\n" +
                "避免内容: " + dto.getAvoidContent();
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

    private NovelInfo convertDtoToEntity(OutlineDTO dto) {
        NovelInfo novelInfo = new NovelInfo();
        novelInfo.setNovelTitle(dto.getNovelTitle());
        novelInfo.setCategory(dto.getCategory());
        novelInfo.setNovelLength(dto.getNovelLength());
        novelInfo.setEndingType(dto.getEndingType());
        novelInfo.setWritingStyle(dto.getWritingStyle());
        novelInfo.setTargetAudience(dto.getTargetAudience());
        novelInfo.setProtagonist(dto.getProtagonist());
        novelInfo.setRoleList(toJson(dto.getRoleList()));
        novelInfo.setBackground(dto.getBackground());
        novelInfo.setWorldRule(dto.getWorldRule());
        novelInfo.setTheme(dto.getTheme());
        novelInfo.setTriggerEvent(dto.getTriggerEvent());
        novelInfo.setForeshadowCount(parseInteger(dto.getForeshadowCount()));
        novelInfo.setNarrativeView(dto.getNarrativeView());
        novelInfo.setAvoidContent(dto.getAvoidContent());
        return novelInfo;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.valueOf(value);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Serialize roleList failed", e);
        }
    }
}
