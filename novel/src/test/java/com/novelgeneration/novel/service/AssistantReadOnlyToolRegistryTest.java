package com.novelgeneration.novel.service;

import com.novelgeneration.novel.mapper.ChapterMapper;
import com.novelgeneration.novel.mapper.HistoryMapper;
import com.novelgeneration.novel.mapper.StoryboardMapper;
import com.novelgeneration.novel.mapper.VideoGenerationTaskMapper;
import com.novelgeneration.novel.mapper.VisualAssetMapper;
import com.novelgeneration.novel.vo.ChapterVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantReadOnlyToolRegistryTest {

    @Mock
    private HistoryMapper historyMapper;
    @Mock
    private ChapterMapper chapterMapper;
    @Mock
    private StoryboardMapper storyboardMapper;
    @Mock
    private VisualAssetMapper visualAssetMapper;
    @Mock
    private VideoGenerationTaskMapper videoGenerationTaskMapper;

    @InjectMocks
    private AssistantReadOnlyToolRegistry registry;

    @Test
    void getNovelChaptersReturnsReadOnlyDataWithSourceContext() {
        ChapterVO chapter = new ChapterVO();
        chapter.setNovelId(54L);
        chapter.setChapterNum(1L);
        chapter.setChapterTitle("表彰大会的嘲讽");
        when(chapterMapper.selectChapter(54L)).thenReturn(List.of(chapter));

        Map<String, Object> result = registry.execute(
                "getNovelChapters",
                Map.of("novelId", 54),
                null);

        assertEquals("getNovelChapters", result.get("tool"));
        assertEquals(true, result.get("success"));
        assertEquals(List.of(chapter), result.get("data"));
        Map<?, ?> source = (Map<?, ?>) result.get("source");
        assertEquals(54L, source.get("novelId"));
        verify(chapterMapper).selectChapter(54L);
    }

    @Test
    void rejectsUnknownToolBeforeAnyDatabaseCall() {
        assertThrows(IllegalArgumentException.class,
                () -> registry.execute("deleteNovel", Map.of("novelId", 54), null));
    }
}
