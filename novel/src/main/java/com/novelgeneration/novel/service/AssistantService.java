package com.novelgeneration.novel.service;

import com.novelgeneration.novel.dto.AssistantChatRequest;
import com.novelgeneration.novel.dto.AssistantChatResponse;

public interface AssistantService {
    AssistantChatResponse chat(AssistantChatRequest request);
}
