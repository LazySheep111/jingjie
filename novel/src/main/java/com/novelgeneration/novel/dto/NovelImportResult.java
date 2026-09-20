package com.novelgeneration.novel.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NovelImportResult {
    private Long novelId;
    private String novelTitle;
    private int chapterCount;
    private List<ChapterPreview> chapters = new ArrayList<>();

    @Data
    public static class ChapterPreview {
        private Integer chapterNum;
        private String chapterTitle;
        private int characterCount;
    }
}
