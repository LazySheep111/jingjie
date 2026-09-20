package com.novelgeneration.novel.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.novelgeneration.novel.dto.NovelDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NovelVO {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long novelId;
    private String novelTitle;
    @JsonAlias("chapterTextList")
    private List<NovelText> novelText;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Data
    public static class NovelText {
        private Integer chapterNum;
        private String chapterTitle;
        private String chapterSummary;
        private String chapterText;
    }
}
