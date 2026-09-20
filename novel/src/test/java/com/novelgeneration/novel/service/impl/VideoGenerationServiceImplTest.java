package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.StoryboardGenerationContext;
import com.novelgeneration.novel.dto.VideoProviderRequest;
import com.novelgeneration.novel.mapper.StoryboardFirstFrameMapper;
import com.novelgeneration.novel.mapper.StoryboardVideoMapper;
import com.novelgeneration.novel.mapper.VideoGenerationTaskMapper;
import com.novelgeneration.novel.service.StoryboardGenerationPreparationService;
import com.novelgeneration.novel.vo.StoryboardFirstFrameVO;
import com.novelgeneration.novel.vo.StoryboardVO;
import com.novelgeneration.novel.vo.StoryboardVideoVO;
import com.novelgeneration.novel.vo.VideoGenerationTaskVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class VideoGenerationServiceImplTest {
    @TempDir
    Path tempDir;

    @Test
    void createTaskRejectsMissingFirstFrameId() {
        VideoGenerationServiceImpl service = new VideoGenerationServiceImpl();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createTask(1L, 2L, 3L, null, null));

        assertEquals("请先选择当前分镜的有效首帧", error.getMessage());
    }

    @Test
    void createTaskSnapshotsSelectedFirstFrameWithoutGeneratingAnother() {
        StoryboardFirstFrameMapper firstFrameMapper = mock(StoryboardFirstFrameMapper.class);
        VideoGenerationTaskMapper taskMapper = mock(VideoGenerationTaskMapper.class);
        StoryboardGenerationPreparationService preparationService = mock(StoryboardGenerationPreparationService.class);
        ExecutorService executor = mock(ExecutorService.class);
        StoryboardFirstFrameVO frame = new StoryboardFirstFrameVO();
        frame.setId(8L);
        frame.setNovelId(1L);
        frame.setChapterNum(2L);
        frame.setSceneId(3L);
        frame.setImagePath("/api/storyboard-frames/files/1/chapter-2/scene-3/a.png");
        when(firstFrameMapper.selectAvailableById(8L)).thenReturn(frame);
        StoryboardGenerationContext context = new StoryboardGenerationContext();
        StoryboardVO.Scene scene = new StoryboardVO.Scene();
        scene.setId(3L);
        context.setScene(scene);
        context.setVideoRequest(new VideoProviderRequest());
        when(preparationService.prepare(1L, 2L, 3L, null)).thenReturn(context);
        doAnswer(invocation -> {
            invocation.getArgument(0, VideoGenerationTaskVO.class).setTaskId(10L);
            return 1;
        }).when(taskMapper).insert(any(VideoGenerationTaskVO.class));

        VideoGenerationServiceImpl service = new VideoGenerationServiceImpl();
        ReflectionTestUtils.setField(service, "firstFrameMapper", firstFrameMapper);
        ReflectionTestUtils.setField(service, "taskMapper", taskMapper);
        ReflectionTestUtils.setField(service, "preparationService", preparationService);
        ReflectionTestUtils.setField(service, "executor", executor);

        service.createTask(1L, 2L, 3L, 8L, null);

        verify(taskMapper).insert(org.mockito.ArgumentMatchers.argThat(task -> task.getFirstFrameId().equals(8L)
                && task.getFirstFramePath().endsWith("a.png")));
    }

    @Test
    void downloadUsesUriOverloadToPreserveSignedQuery() {
        String signedUrl = "https://oss.example.com/output%2Fvideo.mp4?Signature=a%2Fb%3D&Expires=1789387674";
        URI signedUri = URI.create(signedUrl);
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(signedUri, byte[].class)).thenReturn(new byte[]{1, 2, 3});
        VideoGenerationServiceImpl service = new VideoGenerationServiceImpl();
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);

        byte[] result = ReflectionTestUtils.invokeMethod(service, "downloadBytes", signedUrl);

        assertArrayEquals(new byte[]{1, 2, 3}, result);
        verify(restTemplate).getForObject(signedUri, byte[].class);
    }

    @Test
    void uploadCreatesCurrentManualVideoVersion() {
        StoryboardVideoMapper videoMapper = mock(StoryboardVideoMapper.class);
        when(videoMapper.nextVersion(3L)).thenReturn(4);
        VideoGenerationServiceImpl service = uploadService(videoMapper);
        MockMultipartFile video = new MockMultipartFile("file", "scene.mp4", "video/mp4", mp4Bytes());

        StoryboardVideoVO uploaded = service.upload(1L, 2L, 3L, video);

        assertEquals("UPLOAD", uploaded.getSource());
        assertEquals(Boolean.TRUE, uploaded.getCurrent());
        assertEquals(4, uploaded.getVersion());
        verify(videoMapper).clearCurrent(3L);
        verify(videoMapper).insert(uploaded);
    }

    @Test
    void uploadRejectsUnsupportedVideoBeforePersistence() {
        StoryboardVideoMapper videoMapper = mock(StoryboardVideoMapper.class);
        VideoGenerationServiceImpl service = uploadService(videoMapper);

        assertThrows(IllegalArgumentException.class, () -> service.upload(1L, 2L, 3L,
                new MockMultipartFile("file", "scene.avi", "video/x-msvideo", mp4Bytes())));
        assertThrows(IllegalArgumentException.class, () -> service.upload(1L, 2L, 3L,
                new MockMultipartFile("file", "scene.mp4", "video/mp4", new byte[]{1, 2, 3})));

        verify(videoMapper, org.mockito.Mockito.never()).insert(any());
    }

    @Test
    void deleteCurrentVideoPromotesNewestAvailableVersion() {
        StoryboardVideoMapper videoMapper = mock(StoryboardVideoMapper.class);
        StoryboardVideoVO current = new StoryboardVideoVO();
        current.setId(42L);
        current.setSceneId(3L);
        current.setDeleted(false);
        StoryboardVideoVO fallback = new StoryboardVideoVO();
        fallback.setId(41L);
        fallback.setSceneId(3L);
        when(videoMapper.selectById(42L)).thenReturn(current);
        when(videoMapper.selectLatestAvailableByScene(3L)).thenReturn(fallback);
        VideoGenerationServiceImpl service = uploadService(videoMapper);

        StoryboardVideoVO result = service.delete(42L);

        verify(videoMapper).softDelete(42L);
        verify(videoMapper).clearCurrent(3L);
        verify(videoMapper).setCurrent(41L);
        assertEquals(41L, result.getId());
        assertEquals(Boolean.TRUE, result.getCurrent());
    }

    private VideoGenerationServiceImpl uploadService(StoryboardVideoMapper videoMapper) {
        VideoGenerationServiceImpl service = new VideoGenerationServiceImpl();
        ReflectionTestUtils.setField(service, "videoMapper", videoMapper);
        ReflectionTestUtils.setField(service, "downloadDir", tempDir.toString());
        return service;
    }

    private byte[] mp4Bytes() {
        return new byte[]{0, 0, 0, 24, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 0, 0,
                'i', 's', 'o', 'm', 'i', 's', 'o', '2', 'a', 'v', 'c', '1'};
    }

}
