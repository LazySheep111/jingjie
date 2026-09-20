package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.mapper.VisualStyleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.mapper.ChapterMapper;
import com.novelgeneration.novel.utils.AiUtil;
import com.novelgeneration.novel.vo.ChapterVO;
import com.novelgeneration.novel.vo.VisualStyleVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisualStyleServiceImplTest {
    @Mock
    private VisualStyleMapper visualStyleMapper;

    @Mock
    private ChapterMapper chapterMapper;

    @Mock
    private AiUtil aiUtil;

    @InjectMocks
    private VisualStyleServiceImpl service;

    @Test
    void buildPromptIncludesSavedStyleConstraints() {
        VisualStyleVO style = new VisualStyleVO();
        style.setNovelId(53L);
        style.setEra("架空古代东方山村");
        style.setArchitecture("土坯房、灰瓦、木梁");
        style.setNegativePrompt("现代建筑、汽车、电线杆");
        when(visualStyleMapper.selectByNovelId(53L)).thenReturn(style);

        String prompt = service.buildPrompt(53L);

        assertTrue(prompt.contains("时代背景：架空古代东方山村"));
        assertTrue(prompt.contains("建筑风格：土坯房、灰瓦、木梁"));
        assertTrue(prompt.contains("禁止出现：现代建筑、汽车、电线杆"));
    }

    @Test
    void saveCreatesStyleWhenNovelHasNoStyle() {
        VisualStyleVO request = new VisualStyleVO();
        request.setEra("现代");
        when(visualStyleMapper.selectByNovelId(53L)).thenReturn(null, request);

        service.save(53L, request);

        verify(visualStyleMapper).insert(org.mockito.ArgumentMatchers.any(VisualStyleVO.class));
    }

    @Test
    void generateInitialReturnsStyleWithoutSavingIt() {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        ChapterVO chapter = new ChapterVO();
        chapter.setChapterNum(1L);
        chapter.setChapterTitle("河底奇遇");
        chapter.setChapterSummary("少年在山村河边发现神秘石片");
        chapter.setChapterText("清晨的土坯房旁，少年走向青石河。");
        when(chapterMapper.selectChapter(53L)).thenReturn(java.util.List.of(chapter));
        when(visualStyleMapper.selectByNovelId(53L)).thenReturn(null);
        when(aiUtil.chat(org.mockito.ArgumentMatchers.contains("土坯房"))).thenReturn("""
                {"era":"架空古代","region":"东方山村","architecture":"土坯房和灰瓦",
                 "material":"夯土和旧木","colorStyle":"低饱和暖色","lightingStyle":"自然光",
                 "artStyle":"写实电影感","cameraStyle":"电影级构图",
                 "positivePrompt":"保持建筑年代一致","negativePrompt":"现代建筑、汽车"}
                """);

        VisualStyleVO result = service.generateInitial(53L);

        assertTrue(result.getArchitecture().contains("土坯房"));
        verify(visualStyleMapper, org.mockito.Mockito.never())
                .insert(org.mockito.ArgumentMatchers.any(VisualStyleVO.class));
    }
}
