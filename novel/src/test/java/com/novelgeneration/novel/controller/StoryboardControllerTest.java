package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.service.StoryboardService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StoryboardControllerTest {

    @Test
    void removesAnAssetReferenceAtTheCurrentSceneRoute() {
        StoryboardService service = mock(StoryboardService.class);
        StoryboardController controller = new StoryboardController();
        ReflectionTestUtils.setField(controller, "storyboardService", service);

        controller.removeAssetReference(1L, 2L, 3L, 4L, "LOCATION");

        verify(service).removeAssetReference(1L, 2L, 3L, 4L, "LOCATION");
    }
}
