package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.mapper.StoryboardFirstFrameMapper;
import com.novelgeneration.novel.service.StoryboardFrameGenerator;
import com.novelgeneration.novel.vo.StoryboardFirstFrameVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoryboardFirstFrameServiceImplTest {
    @TempDir
    Path tempDir;

    @Test
    void createTaskReturnsExistingRunningFirstFrameTask() {
        StoryboardFirstFrameMapper mapper = mock(StoryboardFirstFrameMapper.class);
        StoryboardFrameGenerator generator = mock(StoryboardFrameGenerator.class);
        StoryboardFirstFrameVO running = new StoryboardFirstFrameVO();
        running.setId(9L);
        running.setStatus("GENERATING");
        when(mapper.selectRunning(1L, 2L, 3L)).thenReturn(running);
        StoryboardFirstFrameServiceImpl service = new StoryboardFirstFrameServiceImpl();
        ReflectionTestUtils.setField(service, "firstFrameMapper", mapper);
        ReflectionTestUtils.setField(service, "storyboardFrameGenerator", generator);

        StoryboardFirstFrameVO result = service.createTask(1L, 2L, 3L);

        assertEquals(9L, result.getId());
        verify(generator, never()).generate(any());
    }

    @Test
    void deleteMarksFirstFrameDeletedWithoutGeneratingOrRemovingFiles() {
        StoryboardFirstFrameMapper mapper = mock(StoryboardFirstFrameMapper.class);
        StoryboardFrameGenerator generator = mock(StoryboardFrameGenerator.class);
        StoryboardFirstFrameVO frame = new StoryboardFirstFrameVO();
        frame.setId(8L);
        when(mapper.selectById(8L)).thenReturn(frame);
        StoryboardFirstFrameServiceImpl service = new StoryboardFirstFrameServiceImpl();
        ReflectionTestUtils.setField(service, "firstFrameMapper", mapper);
        ReflectionTestUtils.setField(service, "storyboardFrameGenerator", generator);

        service.delete(8L);

        verify(mapper).softDelete(8L);
        verify(generator, never()).generate(any());
    }

    @Test
    void uploadCreatesSuccessfulManualFirstFrameVersion() {
        StoryboardFirstFrameMapper mapper = mock(StoryboardFirstFrameMapper.class);
        when(mapper.nextVersion(3L)).thenReturn(7);
        StoryboardFirstFrameServiceImpl service = uploadService(mapper);
        MockMultipartFile file = new MockMultipartFile("file", "frame.png", "image/png", tinyPng());

        StoryboardFirstFrameVO result = service.upload(1L, 2L, 3L, file);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals("UPLOAD", result.getSource());
        assertEquals(7, result.getVersion());
        assertTrue(result.getImagePath().contains("/api/storyboard-frames/files/1/chapter-2/scene-3/"));
        verify(mapper).insert(result);
    }

    @Test
    void uploadRejectsUnsupportedOrUnreadableImagesBeforePersistence() {
        StoryboardFirstFrameMapper mapper = mock(StoryboardFirstFrameMapper.class);
        StoryboardFirstFrameServiceImpl service = uploadService(mapper);

        assertThrows(IllegalArgumentException.class, () -> service.upload(1L, 2L, 3L,
                new MockMultipartFile("file", "frame.gif", "image/gif", tinyPng())));
        assertThrows(IllegalArgumentException.class, () -> service.upload(1L, 2L, 3L,
                new MockMultipartFile("file", "frame.png", "image/png", new byte[]{1, 2, 3})));

        verify(mapper, never()).insert(any());
    }

    private StoryboardFirstFrameServiceImpl uploadService(StoryboardFirstFrameMapper mapper) {
        StoryboardFirstFrameServiceImpl service = new StoryboardFirstFrameServiceImpl();
        ReflectionTestUtils.setField(service, "firstFrameMapper", mapper);
        ReflectionTestUtils.setField(service, "storyboardFrameDir", tempDir.toString());
        return service;
    }

    private byte[] tinyPng() {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", output);
            return output.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
