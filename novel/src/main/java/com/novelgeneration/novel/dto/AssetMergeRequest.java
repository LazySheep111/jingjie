package com.novelgeneration.novel.dto;

import lombok.Data;

@Data
public class AssetMergeRequest {
    private Long targetAssetId;
    private Integer targetVersion;
}
