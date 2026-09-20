package com.novelgeneration.novel.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisualStyleVO {
    private Long id;
    private Long novelId;
    private String era;
    private String region;
    private String architecture;
    private String material;
    private String colorStyle;
    private String lightingStyle;
    private String artStyle;
    private String cameraStyle;
    private String positivePrompt;
    private String negativePrompt;
    private Integer version;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
