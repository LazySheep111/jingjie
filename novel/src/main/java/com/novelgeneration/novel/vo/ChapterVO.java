package com.novelgeneration.novel.vo;

import lombok.Data;

@Data
public class ChapterVO {
    private Long novelId;
    private Long chapterNum;
    private String chapterTitle;
    private String chapterSummary;
    private String chapterText;
}
