package com.novelgeneration.novel.dto;

import lombok.Data;

import java.util.List;

@Data
public class KeysDTO {
    private String novelTitle;
    private String category;
    private String novelLength;
    private String endingType;
    private String writingStyle;
    private String targetAudience;
    private String protagonist;
    private List<?> roleList;
    private String background;
    private String worldRule;
    private String theme;
    private String triggerEvent;
    private String foreshadowCount;
    private  String narrativeView;
    private String avoidContent;
}
