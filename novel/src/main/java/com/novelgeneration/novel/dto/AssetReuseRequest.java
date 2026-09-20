package com.novelgeneration.novel.dto;

import lombok.Data;

@Data
public class AssetReuseRequest {
    private Long targetNovelId;
    private Long targetChapterNum;
    private String assetRole;
    private Long targetAssetId;
}
