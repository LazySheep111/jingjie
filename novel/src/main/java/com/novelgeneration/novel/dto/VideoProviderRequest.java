package com.novelgeneration.novel.dto;

import lombok.Data;

@Data
public class VideoProviderRequest {
    private String prompt;
    private String stylePrompt;
    private Integer durationSec;
    private String aspectRatio = "16:9";
    private String resolution = "768P";
    private String firstFrameImageUrl;
}
