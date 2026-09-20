package com.novelgeneration.novel.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VisualAssetVO {
    private Long assetId;
    private Long versionId;
    private Long novelId;
    private String assetType;
    private String normalizedName;
    private String assetName;
    private Integer version;
    private String coreFeatures;
    private String frontPrompt;
    private String sidePrompt;
    private String backPrompt;
    private String compositeImagePath;
    private String status;
    private Integer reuseCount;
    private String novelTitle;
    private String chapterNumbers;
    private List<Long> chapters;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
