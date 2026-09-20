package com.novelgeneration.novel.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssistantConversationVO {
    private Long id;
    private String conversationId;
    private Long novelId;
    private String title;
    private String lastMessagePreview;
    private Integer messageCount;
    private Long conversationVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
