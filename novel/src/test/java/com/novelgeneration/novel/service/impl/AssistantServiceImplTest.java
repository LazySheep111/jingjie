package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.dto.AssistantChatRequest;
import com.novelgeneration.novel.service.AssistantReadOnlyToolRegistry;
import com.novelgeneration.novel.service.AssistantConversationService;
import com.novelgeneration.novel.vo.AssistantConversationVO;
import com.novelgeneration.novel.utils.AiUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantServiceImplTest {

    @Mock
    private AiUtil aiUtil;
    @Mock
    private AssistantReadOnlyToolRegistry toolRegistry;
    @Mock
    private AssistantConversationService conversationService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private AssistantServiceImpl service;

    @Test
    void plansAndExecutesOnlyTheRegisteredReadOnlyToolBeforeAnswering() {
        AssistantChatRequest request = new AssistantChatRequest();
        request.setMessage("这部小说有几章？");
        request.setNovelId(54L);
        request.setPage("detail");
        AssistantConversationVO conversation = new AssistantConversationVO();
        conversation.setConversationId("conv-54");
        conversation.setNovelId(54L);
        when(conversationService.getOrCreate(54L, "作品 54")).thenReturn(conversation);
        when(conversationService.record(anyString(), eq(54L), anyString(), anyString(), anyString(), any(), any(), any(), any(), eq(1)))
                .thenAnswer(invocation -> {
                    var message = new com.novelgeneration.novel.vo.AssistantMessageVO();
                    message.setMessageId("msg-test");
                    return message;
                });

        when(aiUtil.chatJsonObject(anyString()))
                .thenReturn("{\"tool\":\"getNovelChapters\",\"arguments\":{\"novelId\":54}}");
        Map<String, Object> toolResult = new LinkedHashMap<>();
        toolResult.put("tool", "getNovelChapters");
        toolResult.put("success", true);
        toolResult.put("data", java.util.List.of(Map.of("chapterNum", 1, "chapterTitle", "开场")));
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("novelId", 54L);
        source.put("chapterNum", null);
        toolResult.put("source", source);
        when(toolRegistry.execute("getNovelChapters", Map.of("novelId", 54), request))
                .thenReturn(toolResult);
        when(aiUtil.chat(anyString())).thenReturn("这部小说目前有 1 章。");

        var result = service.chat(request);

        assertEquals("这部小说目前有 1 章。", result.getAnswer());
        assertTrue(result.isReadOnly());
        assertEquals("getNovelChapters", result.getToolCalls().get(0).get("tool"));
        verify(toolRegistry).execute("getNovelChapters", Map.of("novelId", 54), request);
    }
}
