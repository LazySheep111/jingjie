package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.KeysDTO;
import com.novelgeneration.novel.mapper.GenerateOutlineMapper;
import com.novelgeneration.novel.utils.AiUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerateOutlineServiceImplTest {

    @Test
    void generatesOutlineThroughStructuredOutputClient() {
        GenerateOutlineMapper mapper = mock(GenerateOutlineMapper.class);
        AiUtil aiUtil = mock(AiUtil.class);
        GenerateOutlineServiceImpl service = new GenerateOutlineServiceImpl();
        ReflectionTestUtils.setField(service, "generateOutlineMapper", mapper);
        ReflectionTestUtils.setField(service, "aiUtil", aiUtil);

        when(aiUtil.chatJsonObject(anyString()))
                .thenReturn("""
                        {
                          "novelTitle": "测试小说",
                          "totalChapter": 1,
                          "overallPlot": "整体梗概",
                          "foreshadowList": [],
                          "chapterList": [{"chapterNum": 1, "chapterTitle": "第一章", "chapterSummary": "开端", "wordCount": 1000}]
                        }
                        """);

        service.generateOutline(new KeysDTO());

        verify(aiUtil).chatJsonObject(anyString());
        verify(mapper).add(any());
    }

    @Test
    void normalizesObjectForeshadowsFromLegacyProviders() {
        GenerateOutlineMapper mapper = mock(GenerateOutlineMapper.class);
        AiUtil aiUtil = mock(AiUtil.class);
        GenerateOutlineServiceImpl service = new GenerateOutlineServiceImpl();
        ReflectionTestUtils.setField(service, "generateOutlineMapper", mapper);
        ReflectionTestUtils.setField(service, "aiUtil", aiUtil);

        when(aiUtil.chatJsonObject(anyString()))
                .thenReturn("""
                        {
                          "novelTitle": "测试小说",
                          "totalChapter": 1,
                          "overallPlot": "整体梗概",
                          "foreshadowList": [{"id": 1, "setup": "旧笔记本", "payoff": "竞赛中揭示"}],
                          "chapterList": []
                        }
                        """);

        var result = service.generateOutline(new KeysDTO());

        org.junit.jupiter.api.Assertions.assertEquals(
                "铺垫：旧笔记本；回收：竞赛中揭示",
                result.getForeshadowList().get(0));
    }

    @Test
    void rejectsOutlineWhenRequiredFieldHasWrongType() {
        GenerateOutlineMapper mapper = mock(GenerateOutlineMapper.class);
        AiUtil aiUtil = mock(AiUtil.class);
        GenerateOutlineServiceImpl service = new GenerateOutlineServiceImpl();
        ReflectionTestUtils.setField(service, "generateOutlineMapper", mapper);
        ReflectionTestUtils.setField(service, "aiUtil", aiUtil);

        when(aiUtil.chatJsonObject(anyString())).thenReturn("""
                {
                  "novelTitle": "测试小说",
                  "totalChapter": "1",
                  "overallPlot": "整体梗概",
                  "foreshadowList": [],
                  "chapterList": []
                }
                """);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> service.generateOutline(new KeysDTO()));
    }
}
