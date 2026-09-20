package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.VideoGenerationRequest;
import com.novelgeneration.novel.service.StoryboardFirstFrameService;
import com.novelgeneration.novel.service.VideoGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VideoGenerationControllerTest {
    @Test
    void createsFirstFrameTaskAtSceneRoute() {
        StoryboardFirstFrameService firstFrameService = mock(StoryboardFirstFrameService.class);
        VideoGenerationController controller = new VideoGenerationController();
        ReflectionTestUtils.setField(controller, "storyboardFirstFrameService", firstFrameService);

        controller.createFirstFrame(1L, 2L, 3L);

        verify(firstFrameService).createTask(1L, 2L, 3L);
    }

    @Test
    void createsVideoTaskWithExplicitFirstFrame() {
        VideoGenerationService service = mock(VideoGenerationService.class);
        VideoGenerationController controller = new VideoGenerationController();
        ReflectionTestUtils.setField(controller, "videoGenerationService", service);
        VideoGenerationRequest request = new VideoGenerationRequest();
        request.setSceneId(3L);
        request.setFirstFrameId(8L);

        controller.create(1L, 2L, 3L, request);

        verify(service).createTask(1L, 2L, 3L, 8L, null, null);
    }

    @Test
    void retryPassesSafetyRewrittenPromptToExistingFailedTask() {
        VideoGenerationService service = mock(VideoGenerationService.class);
        VideoGenerationController controller = new VideoGenerationController();
        ReflectionTestUtils.setField(controller, "videoGenerationService", service);
        VideoGenerationRequest request = new VideoGenerationRequest();
        request.setPromptOverride("安全改写后的提示词");

        controller.retry(42L, request);

        verify(service).retry(42L, "安全改写后的提示词");
    }

    @Test
    void uploadsFirstFrameAtSceneRoute() {
        StoryboardFirstFrameService firstFrameService = mock(StoryboardFirstFrameService.class);
        VideoGenerationController controller = new VideoGenerationController();
        ReflectionTestUtils.setField(controller, "storyboardFirstFrameService", firstFrameService);
        MockMultipartFile file = new MockMultipartFile("file", "frame.png", "image/png", new byte[]{1});

        controller.uploadFirstFrame(1L, 2L, 3L, file);

        verify(firstFrameService).upload(1L, 2L, 3L, file);
    }

    @Test
    void uploadsAndDeletesManualVideoVersions() {
        VideoGenerationService service = mock(VideoGenerationService.class);
        VideoGenerationController controller = new VideoGenerationController();
        ReflectionTestUtils.setField(controller, "videoGenerationService", service);
        MockMultipartFile file = new MockMultipartFile("file", "scene.mp4", "video/mp4", new byte[]{1});

        controller.uploadVideo(1L, 2L, 3L, file);
        controller.deleteVideo(9L);

        verify(service).upload(1L, 2L, 3L, file);
        verify(service).delete(9L);
    }
}
