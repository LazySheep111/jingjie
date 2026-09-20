package com.novelgeneration.novel.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoryboardFirstFrameVO {
    private Long id;
    private Long novelId;
    private Long chapterNum;
    private Long sceneId;
    private Integer version;
    private String status;
    private String source;
    private String imagePath;
    private String promptSnapshot;
    private String styleSnapshot;
    private String errorMessage;
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
