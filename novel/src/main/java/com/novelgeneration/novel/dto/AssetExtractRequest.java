package com.novelgeneration.novel.dto;

import lombok.Data;

@Data
public class AssetExtractRequest {
    private Long chapterNum;
    private Boolean forceRetry;
}
