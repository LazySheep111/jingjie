package com.novelgeneration.novel.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NovelHistoryItemVO {
    private Long novelId;
    private String novelTitle;
    private Integer totalChapter;
    private String overallPlot;
    private String status;
    private LocalDateTime createTime;
}
