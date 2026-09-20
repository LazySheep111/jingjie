package com.novelgeneration.novel.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class StoryboardVO {
    private Long id;
    private Long novelId;
    private Long chapterNum;
    private Integer version;
    private String status;
    private Integer totalDurationSec;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<Scene> scenes = new ArrayList<>();

    @Data
    public static class Scene {
        private Long id;
        private Integer sequence;
        private Integer durationSec;
        private String location;
        private String timeOfDay;
        private String weather;
        private String characters;
        private String shotType;
        private String cameraMovement;
        private String shotPlan;
        private String characterEmotion;
        private String voiceOver;
        private String transition;
        private String imagePrompt;
        private List<Long> characterAssetIds = new ArrayList<>();
        private List<Long> locationAssetIds = new ArrayList<>();
    }
}
