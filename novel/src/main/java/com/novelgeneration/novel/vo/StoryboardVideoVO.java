package com.novelgeneration.novel.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class StoryboardVideoVO {
    private Long id;
    private Long novelId;
    private Long chapterNum;
    private Long sceneId;
    private Integer version;
    private String source;
    private String videoPath;
    private String firstFramePath;
    private Integer durationSec;
    private String resolution;
    private String promptSnapshot;
    private String styleSnapshot;
    private Boolean current;
    private Boolean deleted;
    private LocalDateTime createTime;
    private List<AssetSnapshot> assets = new ArrayList<>();

    @Data
    public static class AssetSnapshot {
        private Long videoId;
        private Long assetId;
        private Integer assetVersion;
        private String assetName;
        private String assetType;
        private String imagePath;
    }
}
