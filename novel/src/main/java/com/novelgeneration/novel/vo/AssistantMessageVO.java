package com.novelgeneration.novel.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssistantMessageVO {
    private Long id;
    private String messageId;
    private String conversationId;
    private Long sequenceNo;
    private String role;
    private String content;
    private String status;
    private String toolName;
    private String toolArguments;
    private String toolResult;
    private String parentMessageId;
    private Integer attemptNo;
    private LocalDateTime createdAt;
    private LocalDateTime persistedAt;
}
