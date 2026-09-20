package com.novelgeneration.novel.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class OutlineDTO {
    private Long novelId;
    private FormData formData;
    private Outline outline;
    private Boolean startFullGeneration;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String roleListJson;
    private String foreshadowListJson;
    private String chapterListJson;

    @Data
    public static class FormData {
        private String novelTitle;
        private String category;
        private String protagonist;
        private List<String> roleList;
    }

    @Data
    public static class Outline {
        private String novelTitle;
        private String overallPlot;
        private List<String> foreshadowList;
        private List<ChapterItem> chapterList;
    }

    @Data
    public static class ChapterItem {
        private Integer chapterNum;
        private String chapterTitle;
        private String chapterSummary;
        private Integer wordCount;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public FormData getFormData() {
        return formData;
    }

    public void setFormData(FormData formData) {
        this.formData = formData;
    }

    public Outline getOutline() {
        return outline;
    }

    public void setOutline(Outline outline) {
        this.outline = outline;
    }

    public Boolean getStartFullGeneration() {
        return startFullGeneration;
    }

    public void setStartFullGeneration(Boolean startFullGeneration) {
        this.startFullGeneration = startFullGeneration;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getRoleListJson() {
        return roleListJson;
    }

    public void setRoleListJson(String roleListJson) {
        this.roleListJson = roleListJson;
    }

    public String getForeshadowListJson() {
        return foreshadowListJson;
    }

    public void setForeshadowListJson(String foreshadowListJson) {
        this.foreshadowListJson = foreshadowListJson;
    }

    public String getChapterListJson() {
        return chapterListJson;
    }

    public void setChapterListJson(String chapterListJson) {
        this.chapterListJson = chapterListJson;
    }
}
