package com.novelgeneration.novel.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StoryboardFrameRequest {
    private Long novelId;
    private Long chapterNum;
    private Long sceneId;
    private String prompt;
    private List<String> referenceImageUrls = new ArrayList<>();
}
