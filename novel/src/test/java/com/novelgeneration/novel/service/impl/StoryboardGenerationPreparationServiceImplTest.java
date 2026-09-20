package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.StoryboardGenerationContext;
import com.novelgeneration.novel.service.StoryboardService;
import com.novelgeneration.novel.service.VisualAssetService;
import com.novelgeneration.novel.service.VisualStyleService;
import com.novelgeneration.novel.vo.StoryboardVO;
import com.novelgeneration.novel.vo.VisualAssetVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StoryboardGenerationPreparationServiceImplTest {
    @Test
    void prepareBuildsFrameAndVideoRequestsFromTheSameScene() {
        StoryboardService storyboardService = mock(StoryboardService.class);
        VisualAssetService assetService = mock(VisualAssetService.class);
        VisualStyleService styleService = mock(VisualStyleService.class);
        when(storyboardService.getLatest(1L, 2L)).thenReturn(storyboard());
        when(styleService.buildPrompt(1L)).thenReturn("东方玄幻写实");
        when(assetService.get(1L, 7L)).thenReturn(asset(7L, "CHARACTER", "凌程", "/images/lingcheng.png"));
        when(assetService.get(1L, 8L)).thenReturn(asset(8L, "LOCATION", "青石河", "/images/river.png"));

        StoryboardGenerationPreparationServiceImpl service = new StoryboardGenerationPreparationServiceImpl();
        ReflectionTestUtils.setField(service, "storyboardService", storyboardService);
        ReflectionTestUtils.setField(service, "visualAssetService", assetService);
        ReflectionTestUtils.setField(service, "visualStyleService", styleService);
        ReflectionTestUtils.setField(service, "imageBaseUrl", "https://example.test");

        StoryboardGenerationContext context = service.prepare(1L, 2L, 3L, null);

        assertEquals(3L, context.getScene().getId());
        assertEquals(10, context.getVideoRequest().getDurationSec());
        assertTrue(context.getFrameRequest().getPrompt().contains("单张16:9完整影视画面"));
        assertTrue(context.getFrameRequest().getPrompt().contains("东方玄幻写实"));
        assertEquals(List.of("https://example.test/images/lingcheng.png", "https://example.test/images/river.png"),
                context.getFrameRequest().getReferenceImageUrls());
        assertEquals(2, context.getAssetSnapshots().size());
    }

    private StoryboardVO storyboard() {
        StoryboardVO.Scene scene = new StoryboardVO.Scene();
        scene.setId(3L);
        scene.setDurationSec(10);
        scene.setLocation("青石河边");
        scene.setTimeOfDay("傍晚");
        scene.setShotType("中景");
        scene.setCameraMovement("缓慢推进");
        scene.setShotPlan("时长 0:00-0:04\n[0:00-0:04 | 中景]：凌程站在河边，抬头看向远方。");
        scene.setCharacterAssetIds(List.of(7L));
        scene.setLocationAssetIds(List.of(8L));
        StoryboardVO storyboard = new StoryboardVO();
        storyboard.setScenes(List.of(scene));
        return storyboard;
    }

    private VisualAssetVO asset(Long id, String type, String name, String path) {
        VisualAssetVO asset = new VisualAssetVO();
        asset.setAssetId(id);
        asset.setAssetType(type);
        asset.setAssetName(name);
        asset.setVersion(1);
        asset.setCompositeImagePath(path);
        return asset;
    }
}
