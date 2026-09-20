package com.novelgeneration.novel.vo;

import lombok.Data;

@Data
public class HistoryOutlineRecordVO {
    private Long novelId;
    private String novelTitle;
    private String overallPlot;
    private String foreshadowListJson;
    private String chapterListJson;
    private Boolean startFullGeneration;
}
