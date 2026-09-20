package com.novelgeneration.novel.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

public class NovelDTO {
    private Long novelId;
    private String novelTitle;
    private List<NovelText> novelText;

    @Data
    public static class NovelText {
        private Integer chapterNum;
        private String chapterTitle;
        private String chapterSummary;
        private String chapterText;
    }
}
