package com.novelgeneration.novel.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NovelInfo {
    private Long id;
    private String novelTitle;
    private String category;
    // 对应数据库 novel_length，Java采用驼峰命名
    private String novelLength;
    private String endingType;
    private String writingStyle;
    private String targetAudience;
    private String protagonist;
    private String roleList;
    private String background;
    private String worldRule;
    private String theme;
    private String triggerEvent;
    private Integer foreshadowCount;
    private String narrativeView;
    private String avoidContent;

    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNovelTitle() {
        return novelTitle;
    }

    public void setNovelTitle(String novelTitle) {
        this.novelTitle = novelTitle;
    }

    public String getNovelLength() {
        return novelLength;
    }

    public void setNovelLength(String novelLength) {
        this.novelLength = novelLength;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEndingType() {
        return endingType;
    }

    public void setEndingType(String endingType) {
        this.endingType = endingType;
    }

    public String getWritingStyle() {
        return writingStyle;
    }

    public void setWritingStyle(String writingStyle) {
        this.writingStyle = writingStyle;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public String getProtagonist() {
        return protagonist;
    }

    public void setProtagonist(String protagonist) {
        this.protagonist = protagonist;
    }

    public String getRoleList() {
        return roleList;
    }

    public void setRoleList(String roleList) {
        this.roleList = roleList;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getWorldRule() {
        return worldRule;
    }

    public void setWorldRule(String worldRule) {
        this.worldRule = worldRule;
    }

    public String getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(String triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    public Integer getForeshadowCount() {
        return foreshadowCount;
    }

    public void setForeshadowCount(Integer foreshadowCount) {
        this.foreshadowCount = foreshadowCount;
    }

    public String getNarrativeView() {
        return narrativeView;
    }

    public void setNarrativeView(String narrativeView) {
        this.narrativeView = narrativeView;
    }

    public String getAvoidContent() {
        return avoidContent;
    }

    public void setAvoidContent(String avoidContent) {
        this.avoidContent = avoidContent;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
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
}
