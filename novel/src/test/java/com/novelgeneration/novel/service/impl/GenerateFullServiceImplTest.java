package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.OutlineDTO;
import com.novelgeneration.novel.mapper.NovelTextMapper;
import com.novelgeneration.novel.utils.AiUtil;
import com.novelgeneration.novel.vo.NovelVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerateFullServiceImplTest {

    @Test
    void generatesAndSavesNovelOneChapterAtATime() {
        NovelTextMapper novelTextMapper = mock(NovelTextMapper.class);
        AiUtil aiUtil = mock(AiUtil.class);
        GenerateFullServiceImpl service = new GenerateFullServiceImpl();
        ReflectionTestUtils.setField(service, "novelTextMapper", novelTextMapper);
        ReflectionTestUtils.setField(service, "aiUtil", aiUtil);

        when(aiUtil.chatJsonObject(any(String.class)))
                .thenReturn("""
                        {
                          "chapterNum": 1,
                          "chapterTitle": "第一章",
                          "chapterSummary": "第一章概要",
                          "chapterText": "第一章正文"
                        }
                        """)
                .thenReturn("""
                        {
                          "chapterNum": 2,
                          "chapterTitle": "第二章",
                          "chapterSummary": "第二章概要",
                          "chapterText": "第二章正文"
                        }
                        """);

        service.GenerateFull(47L, buildOutlineDTO());

        verify(aiUtil, times(2)).chatJsonObject(any(String.class));
        verify(novelTextMapper, never()).deleteByNovelId(47L);
        verify(novelTextMapper, times(2)).insertChapter(eq(47L), any(NovelVO.NovelText.class), any(), any());
    }

    @Test
    void continuesFromNextMissingChapter() {
        NovelTextMapper novelTextMapper = mock(NovelTextMapper.class);
        AiUtil aiUtil = mock(AiUtil.class);
        GenerateFullServiceImpl service = new GenerateFullServiceImpl();
        ReflectionTestUtils.setField(service, "novelTextMapper", novelTextMapper);
        ReflectionTestUtils.setField(service, "aiUtil", aiUtil);

        NovelVO.NovelText existingChapter = new NovelVO.NovelText();
        existingChapter.setChapterNum(1);
        when(novelTextMapper.selectNovelText(48L)).thenReturn(java.util.List.of(existingChapter));
        when(aiUtil.chatJsonObject(any(String.class))).thenReturn("""
                {
                  "chapterNum": 2,
                  "chapterTitle": "第二章",
                  "chapterSummary": "第二章概要",
                  "chapterText": "第二章正文"
                }
                """);

        service.GenerateFull(48L, buildOutlineDTO());

        verify(novelTextMapper, never()).deleteByNovelId(48L);
        verify(aiUtil, times(1)).chatJsonObject(any(String.class));
        verify(novelTextMapper, times(1)).insertChapter(eq(48L), any(NovelVO.NovelText.class), any(), any());
    }

    @Test
    void recordsFailedStatusWhenChapterGenerationFails() {
        NovelTextMapper novelTextMapper = mock(NovelTextMapper.class);
        AiUtil aiUtil = mock(AiUtil.class);
        GenerateFullServiceImpl service = new GenerateFullServiceImpl();
        ReflectionTestUtils.setField(service, "novelTextMapper", novelTextMapper);
        ReflectionTestUtils.setField(service, "aiUtil", aiUtil);

        when(novelTextMapper.selectNovelText(49L)).thenReturn(java.util.Collections.emptyList());
        when(aiUtil.chatJsonObject(any(String.class))).thenThrow(new RuntimeException("AI timeout"));

        try {
            service.GenerateFull(49L, buildOutlineDTO());
        } catch (RuntimeException ignored) {
        }

        assert "FAILED".equals(GenerateFullServiceImpl.getGenerationState(49L).getStatus());
    }

    @Test
    void asksAiToRepairInvalidJsonBeforeFailingChapter() {
        NovelTextMapper novelTextMapper = mock(NovelTextMapper.class);
        AiUtil aiUtil = mock(AiUtil.class);
        GenerateFullServiceImpl service = new GenerateFullServiceImpl();
        ReflectionTestUtils.setField(service, "novelTextMapper", novelTextMapper);
        ReflectionTestUtils.setField(service, "aiUtil", aiUtil);

        when(novelTextMapper.selectNovelText(51L)).thenReturn(java.util.Collections.emptyList());
        when(aiUtil.chatJsonObject(any(String.class)))
                .thenReturn("This is plain chapter text, not JSON.")
                .thenReturn("""
                        {
                          "chapterNum": 1,
                          "chapterTitle": "Chapter One",
                          "chapterSummary": "Summary One",
                          "chapterText": "Repaired chapter text"
                        }
                        """)
                .thenReturn("""
                        {
                          "chapterNum": 2,
                          "chapterTitle": "Chapter Two",
                          "chapterSummary": "Summary Two",
                          "chapterText": "Chapter two text"
                        }
                        """);

        service.GenerateFull(51L, buildOutlineDTO());

        verify(aiUtil, times(3)).chatJsonObject(any(String.class));
        verify(novelTextMapper, times(2)).insertChapter(eq(51L), any(NovelVO.NovelText.class), any(), any());
    }

    @Test
    void rejectsChapterWhenSchemaTypeIsInvalidBeforeSaving() {
        NovelTextMapper novelTextMapper = mock(NovelTextMapper.class);
        AiUtil aiUtil = mock(AiUtil.class);
        GenerateFullServiceImpl service = new GenerateFullServiceImpl();
        ReflectionTestUtils.setField(service, "novelTextMapper", novelTextMapper);
        ReflectionTestUtils.setField(service, "aiUtil", aiUtil);

        when(novelTextMapper.selectNovelText(52L)).thenReturn(java.util.Collections.emptyList());
        when(aiUtil.chatJsonObject(any(String.class))).thenReturn("""
                {
                  "chapterNum": "1",
                  "chapterTitle": "第一章",
                  "chapterSummary": "摘要",
                  "chapterText": "正文"
                }
                """);

        try {
            service.GenerateFull(52L, buildOutlineDTO());
        } catch (RuntimeException ignored) {
        }

        verify(novelTextMapper, never()).insertChapter(eq(52L), any(NovelVO.NovelText.class), any(), any());
        assert "FAILED".equals(GenerateFullServiceImpl.getGenerationState(52L).getStatus());
    }

    private OutlineDTO buildOutlineDTO() {
        OutlineDTO dto = new OutlineDTO();
        OutlineDTO.Outline outline = new OutlineDTO.Outline();
        outline.setNovelTitle("星海归途");
        outline.setOverallPlot("整体梗概");
        outline.setChapterList(java.util.List.of(chapter(1, "第一章"), chapter(2, "第二章")));
        dto.setOutline(outline);
        return dto;
    }

    private OutlineDTO.ChapterItem chapter(Integer chapterNum, String chapterTitle) {
        OutlineDTO.ChapterItem item = new OutlineDTO.ChapterItem();
        item.setChapterNum(chapterNum);
        item.setChapterTitle(chapterTitle);
        item.setChapterSummary(chapterTitle + "概要");
        item.setWordCount(2000);
        return item;
    }
}
