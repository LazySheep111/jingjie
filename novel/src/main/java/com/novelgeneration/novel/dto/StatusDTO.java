package com.novelgeneration.novel.dto;

import lombok.Data;

@Data
public class StatusDTO {
    private Long novelId;
    private String novelTitle;
    private String status;
    private Long currentChapter;
    private Long totalChapter;
}
