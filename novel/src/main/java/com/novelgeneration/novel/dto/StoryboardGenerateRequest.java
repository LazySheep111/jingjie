package com.novelgeneration.novel.dto;

import lombok.Data;

@Data
public class StoryboardGenerateRequest {
    private Long novelId;
    private Long chapterNum;
    private String chapterTitle;
    private String chapterText;
    private Integer targetDurationSec = 10;
    private String visualStyle = "电影感写实";
    private String aspectRatio = "16:9";
}
