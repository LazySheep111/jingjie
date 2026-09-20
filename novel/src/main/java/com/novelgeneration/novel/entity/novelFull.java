package com.novelgeneration.novel.entity;

import com.novelgeneration.novel.dto.OutlineDTO;
import lombok.Data;

import java.util.List;

public class novelFull {

    private  long novelId;
    private String novelTitle;
    private String overallPlot;
    private List<ChapterFull> chapterTextList;


    @Data
    public static class ChapterFull {
        private Integer chapterNum; //章节编号
        private String chapterTitle; //章节标题
        private String chapterText;  //章节具体内容
        private Integer wordCount;  //章节字数
    }
}
