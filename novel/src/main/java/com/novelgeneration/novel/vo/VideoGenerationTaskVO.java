package com.novelgeneration.novel.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoGenerationTaskVO {
    private Long taskId;
    private Long novelId;
    private Long chapterNum;
    private Long sceneId;
    private Long firstFrameId;
    private String resolution;
    private String status;
    private String providerTaskId;
    private String firstFramePath;
    private Long videoId;
    private String videoUrl;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
