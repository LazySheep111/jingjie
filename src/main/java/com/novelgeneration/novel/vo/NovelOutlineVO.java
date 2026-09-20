package com.novelgeneration.novel.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class NovelOutlineVO {
    private Long novelId;
    private String novelTitle;
    private Integer totalChapter;
    private String overallPlot;
    private List<String> foreshadowList;
    private List<ChapterOutlineVO> chapterList;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class ChapterOutlineVO {
        private Integer chapterNum;
        private String chapterTitle;
        private String chapterSummary;
        private Integer wordCount;
    }
}
