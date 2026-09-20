package com.novelgeneration.novel.vo;

import lombok.Data;

@Data
public class StatusVo {
    private Long novelId;
    private String novelTitle;
    private String status;
    private Long currentChapter;
    private Long totalChapter;
    private String errorMessage;
}
