package com.novelgeneration.novel.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompositeImageTaskVO {
    private String taskId;
    private Long novelId;
    private Long assetId;
    private Integer assetVersion;
    private String status;
    private String imagePath;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
