package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.StoryboardFrameRequest;
import com.novelgeneration.novel.dto.StoryboardGenerationContext;
import com.novelgeneration.novel.dto.VideoProviderRequest;
import com.novelgeneration.novel.service.StoryboardGenerationPreparationService;
import com.novelgeneration.novel.service.StoryboardService;
import com.novelgeneration.novel.service.VisualAssetService;
import com.novelgeneration.novel.service.VisualStyleService;
import com.novelgeneration.novel.vo.StoryboardVO;
import com.novelgeneration.novel.vo.StoryboardVideoVO;
import com.novelgeneration.novel.vo.VisualAssetVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class StoryboardGenerationPreparationServiceImpl implements StoryboardGenerationPreparationService {
    @Resource
    private StoryboardService storyboardService;

    @Resource
    private VisualAssetService visualAssetService;

    @Resource
    private VisualStyleService visualStyleService;

    @Value("${video.generation.image-base-url:}")
    private String imageBaseUrl;

    @Override
    public StoryboardGenerationContext prepare(Long novelId, Long chapterNum, Long sceneId, String promptOverride) {
        requireId(novelId, "novelId");
        requireId(chapterNum, "chapterNum");
        requireId(sceneId, "sceneId");
        StoryboardVO storyboard = storyboardService.getLatest(novelId, chapterNum);
        if (storyboard == null) {
            throw new IllegalArgumentException("当前章节还没有分镜脚本");
        }
        StoryboardVO.Scene scene = storyboard.getScenes().stream()
                .filter(item -> sceneId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("分镜不存在"));
        String stylePrompt = visualStyleService.buildPrompt(novelId);

        VideoProviderRequest videoRequest = new VideoProviderRequest();
        videoRequest.setPrompt(blank(promptOverride) ? buildScenePrompt(scene) : promptOverride.trim());
        videoRequest.setStylePrompt(stylePrompt);
        videoRequest.setDurationSec(scene.getDurationSec() == null ? 10 : scene.getDurationSec());

        StoryboardFrameRequest frameRequest = new StoryboardFrameRequest();
        frameRequest.setNovelId(novelId);
        frameRequest.setChapterNum(chapterNum);
        frameRequest.setSceneId(sceneId);
        frameRequest.setPrompt(buildFirstFramePrompt(scene, stylePrompt));

        StoryboardGenerationContext context = new StoryboardGenerationContext();
        context.setScene(scene);
        context.setStylePrompt(stylePrompt);
        context.setVideoRequest(videoRequest);
        context.setFrameRequest(frameRequest);
        addAssets(novelId, scene.getCharacterAssetIds(), "CHARACTER", context);
        addAssets(novelId, scene.getLocationAssetIds(), "LOCATION", context);
        if (frameRequest.getReferenceImageUrls().isEmpty()) {
            throw new IllegalArgumentException("当前分镜没有关联人物或场景三视图");
        }
        return context;
    }

    @Override
    public String toProviderImagePath(String path) {
        if (path != null && (path.startsWith("http://") || path.startsWith("https://"))) {
            return path;
        }
        if (blank(imageBaseUrl)) {
            throw new IllegalArgumentException("参考三视图不是公网地址，请配置 video.generation.image-base-url");
        }
        return imageBaseUrl.replaceAll("/$", "") + "/" + String.valueOf(path).replaceFirst("^/", "");
    }

    private void addAssets(Long novelId, List<Long> ids, String type, StoryboardGenerationContext context) {
        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            VisualAssetVO asset = visualAssetService.get(novelId, id);
            if (asset == null || blank(asset.getCompositeImagePath())) {
                String name = asset == null || blank(asset.getAssetName()) ? String.valueOf(id) : asset.getAssetName();
                throw new IllegalArgumentException("资产“" + name + "”缺少三视图，请先生成三视图");
            }
            context.getFrameRequest().getReferenceImageUrls().add(toProviderImagePath(asset.getCompositeImagePath()));
            StoryboardVideoVO.AssetSnapshot snapshot = new StoryboardVideoVO.AssetSnapshot();
            snapshot.setAssetId(asset.getAssetId());
            snapshot.setAssetName(asset.getAssetName());
            snapshot.setAssetType(type);
            snapshot.setAssetVersion(asset.getVersion());
            snapshot.setImagePath(asset.getCompositeImagePath());
            context.getAssetSnapshots().add(snapshot);
        }
    }

    private String buildScenePrompt(StoryboardVO.Scene scene) {
        return "镜头脚本：\n" + value(scene.getShotPlan());
    }

    private String buildFirstFramePrompt(StoryboardVO.Scene scene, String stylePrompt) {
        return "请生成一张单张16:9完整影视画面，作为该分镜视频的第一帧。"
                + "不要生成三视图、设定稿、拼图、边框或文字标签。"
                + "严格保持参考图中人物的面部、发型、服装与体型，以及场景的建筑、材质和时代特征。\n"
                + "全书视觉风格：" + value(stylePrompt) + "\n"
                + "场景与时间：" + value(scene.getLocation()) + "，" + value(scene.getTimeOfDay()) + "\n"
                + "镜头脚本：\n" + value(scene.getShotPlan()) + "\n"
                + "镜头与构图补充：" + value(scene.getShotType()) + "，" + value(scene.getCameraMovement());
    }

    private void requireId(Long value, String name) {
        if (value == null || value < 1) {
            throw new IllegalArgumentException(name + " 必须大于 0");
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
