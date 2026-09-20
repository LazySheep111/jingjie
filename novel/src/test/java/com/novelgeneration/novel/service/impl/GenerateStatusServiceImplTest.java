package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.mapper.GenerateOutlineMapper;
import com.novelgeneration.novel.mapper.NovelTextMapper;
import com.novelgeneration.novel.vo.NovelVO;
import com.novelgeneration.novel.vo.StatusVo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GenerateStatusServiceImplTest {

    @Test
    void returnsFullGeneratedStatusWhenAllChaptersExist() {
        NovelTextMapper novelTextMapper = mock(NovelTextMapper.class);
        GenerateOutlineMapper generateOutlineMapper = mock(GenerateOutlineMapper.class);
        GenerateStatusServiceImpl service = new GenerateStatusServiceImpl();

        NovelVO novelVO = new NovelVO();
        List<NovelVO.NovelText> novelText = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            NovelVO.NovelText chapter = new NovelVO.NovelText();
            chapter.setChapterNum(i);
            chapter.setChapterTitle("第 " + i + " 章");
            chapter.setChapterText("正文");
            novelText.add(chapter);
        }
        novelVO.setNovelText(novelText);

        when(novelTextMapper.selectNovelText(28L)).thenReturn(novelVO.getNovelText());
        when(generateOutlineMapper.selectNovelTitle(28L)).thenReturn("星海归途");
        when(generateOutlineMapper.selectTotalChapter(28L)).thenReturn(20L);
        ReflectionTestUtils.setField(service, "novelTextMapper", novelTextMapper);
        ReflectionTestUtils.setField(service, "generateOutlineMapper", generateOutlineMapper);

        Result result = service.generatestaus(28L);
        StatusVo statusVo = (StatusVo) result.getData();

        assertEquals(28L, statusVo.getNovelId());
        assertEquals("星海归途", statusVo.getNovelTitle());
        assertEquals("FULL_GENERATED", statusVo.getStatus());
        assertEquals(20L, statusVo.getCurrentChapter());
        assertEquals(20L, statusVo.getTotalChapter());
    }

    @Test
    void returnsFailedStatusWhenGenerationStateFailed() {
        NovelTextMapper novelTextMapper = mock(NovelTextMapper.class);
        GenerateOutlineMapper generateOutlineMapper = mock(GenerateOutlineMapper.class);
        GenerateStatusServiceImpl service = new GenerateStatusServiceImpl();

        GenerateFullServiceImpl.markGenerationFailed(50L, 3L, "AI timeout");
        when(novelTextMapper.selectNovelText(50L)).thenReturn(List.of());
        when(generateOutlineMapper.selectNovelTitle(50L)).thenReturn("星海归途");
        when(generateOutlineMapper.selectTotalChapter(50L)).thenReturn(20L);
        ReflectionTestUtils.setField(service, "novelTextMapper", novelTextMapper);
        ReflectionTestUtils.setField(service, "generateOutlineMapper", generateOutlineMapper);

        Result result = service.generatestaus(50L);
        StatusVo statusVo = (StatusVo) result.getData();

        assertEquals("FAILED", statusVo.getStatus());
        assertEquals(3L, statusVo.getCurrentChapter());
        assertEquals("AI timeout", statusVo.getErrorMessage());
    }

    @Test
    void treatsImportedNovelWithoutOutlineAsFullyGenerated() {
        NovelTextMapper novelTextMapper = mock(NovelTextMapper.class);
        GenerateOutlineMapper generateOutlineMapper = mock(GenerateOutlineMapper.class);
        GenerateStatusServiceImpl service = new GenerateStatusServiceImpl();

        List<NovelVO.NovelText> importedChapters = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            NovelVO.NovelText chapter = new NovelVO.NovelText();
            chapter.setChapterNum(i);
            chapter.setChapterTitle("第 " + i + " 章");
            chapter.setChapterText("导入正文");
            importedChapters.add(chapter);
        }
        when(novelTextMapper.selectNovelText(66L)).thenReturn(importedChapters);
        when(generateOutlineMapper.selectNovelTitle(66L)).thenReturn(null);
        when(generateOutlineMapper.selectTotalChapter(66L)).thenReturn(null);
        ReflectionTestUtils.setField(service, "novelTextMapper", novelTextMapper);
        ReflectionTestUtils.setField(service, "generateOutlineMapper", generateOutlineMapper);

        Result result = service.generatestaus(66L);
        StatusVo statusVo = (StatusVo) result.getData();

        assertEquals("FULL_GENERATED", statusVo.getStatus());
        assertEquals(6L, statusVo.getCurrentChapter());
        assertEquals(6L, statusVo.getTotalChapter());
    }
}
