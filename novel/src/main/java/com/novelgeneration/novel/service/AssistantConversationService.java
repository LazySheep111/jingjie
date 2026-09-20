package com.novelgeneration.novel.service;

import com.novelgeneration.novel.vo.AssistantConversationVO;
import com.novelgeneration.novel.vo.AssistantMessageVO;

import java.util.List;

public interface AssistantConversationService {
    AssistantConversationVO getOrCreate(Long novelId, String title);

    List<AssistantMessageVO> recentMessages(AssistantConversationVO conversation, int rounds);

    AssistantMessageVO record(String conversationId, Long novelId, String role, String content,
                               String status, String toolName, String toolArguments, String toolResult,
                               String parentMessageId, int attemptNo);

    List<AssistantConversationVO> list(int page, int size);

    long count();

    void delete(String conversationId);
}
