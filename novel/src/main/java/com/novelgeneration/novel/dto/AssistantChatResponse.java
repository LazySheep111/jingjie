package com.novelgeneration.novel.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class AssistantChatResponse {
    private String conversationId;
    private String messageId;
    private String answer;
    private boolean readOnly = true;
    private List<Map<String, Object>> toolCalls = new ArrayList<>();
    private List<Map<String, Object>> sources = new ArrayList<>();

    public AssistantChatResponse() {
    }

    public AssistantChatResponse(String answer) {
        this.answer = answer;
    }
}
