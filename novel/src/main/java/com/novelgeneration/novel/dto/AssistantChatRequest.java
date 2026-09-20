package com.novelgeneration.novel.dto;

import lombok.Data;

@Data
public class AssistantChatRequest {
    private String conversationId;
    private String message;
    private Long novelId;
    private Long chapterNum;
    private Long sceneId;
    private Long taskId;
    private String page;
}
