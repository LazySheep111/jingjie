package com.novelgeneration.novel.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssetTaskVO {
    private String taskId;
    private Long novelId;
    private Long chapterNum;
    private String status;
    private Integer total;
    private Integer reused;
    private Integer created;
    private Integer failed;
    private String message;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
