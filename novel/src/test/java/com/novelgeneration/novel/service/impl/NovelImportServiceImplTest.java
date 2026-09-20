package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.mapper.GenerateOutlineMapper;
import com.novelgeneration.novel.mapper.NovelTextMapper;
import com.novelgeneration.novel.dto.NovelImportResult;
import com.novelgeneration.novel.entity.NovelInfo;
import com.novelgeneration.novel.vo.NovelVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentCaptor.forClass;

class NovelImportServiceImplTest {
    @TempDir
    Path tempDir;

    @Test
    void importsTxtAndSplitsChineseChapterHeadings() {
        GenerateOutlineMapper outlineMapper = mock(GenerateOutlineMapper.class);
        NovelTextMapper textMapper = mock(NovelTextMapper.class);
        doAnswer(invocation -> {
            NovelInfo novel = invocation.getArgument(0);
            novel.setId(88L);
            return null;
        }).when(outlineMapper).add(any(NovelInfo.class));

        NovelImportServiceImpl service = service(outlineMapper, textMapper);
        MockMultipartFile file = new MockMultipartFile(
                "file", "归途.txt", "text/plain",
                "第一章 河边\n少年走到河边。\n\n第二章 夜色\n夜幕降临。".getBytes(StandardCharsets.UTF_8));

        NovelImportResult result = service.importNovel(file, null);

        assertEquals(88L, result.getNovelId());
        assertEquals("归途", result.getNovelTitle());
        assertEquals(2, result.getChapterCount());
        assertEquals("第一章 河边", result.getChapters().get(0).getChapterTitle());
        ArgumentCaptor<NovelInfo> novelCaptor = forClass(NovelInfo.class);
        verify(outlineMapper).add(novelCaptor.capture());
        assertEquals("[]", novelCaptor.getValue().getRoleList());
        verify(textMapper, org.mockito.Mockito.times(2)).insertChapter(any(Long.class), any(NovelVO.NovelText.class), any(), any());
    }

    @Test
    void rejectsUnsupportedFileType() {
        NovelImportServiceImpl service = service(mock(GenerateOutlineMapper.class), mock(NovelTextMapper.class));
        MockMultipartFile file = new MockMultipartFile("file", "novel.pdf", "application/pdf", new byte[]{1});

        assertThrows(IllegalArgumentException.class, () -> service.importNovel(file, null));
    }

    private NovelImportServiceImpl service(GenerateOutlineMapper outlineMapper, NovelTextMapper textMapper) {
        NovelImportServiceImpl service = new NovelImportServiceImpl();
        ReflectionTestUtils.setField(service, "generateOutlineMapper", outlineMapper);
        ReflectionTestUtils.setField(service, "novelTextMapper", textMapper);
        ReflectionTestUtils.setField(service, "importDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "maxBytes", 50L * 1024 * 1024);
        return service;
    }
}
