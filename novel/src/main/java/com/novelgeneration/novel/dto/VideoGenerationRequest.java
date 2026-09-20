package com.novelgeneration.novel.dto;

import lombok.Data;

@Data
public class VideoGenerationRequest {
    private Long sceneId;
    private Long firstFrameId;
    private String promptOverride;
    private String resolution;
}
