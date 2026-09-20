package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.service.AssistantMessageEventPublisher;
import com.novelgeneration.novel.vo.AssistantMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssistantMessageEventPublisherImpl implements AssistantMessageEventPublisher {
    public static final String STREAM = "assistant:message-events";
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void publish(AssistantMessageVO message) {
        Map<String, String> event = new LinkedHashMap<>();
        event.put("messageId", message.getMessageId());
        event.put("conversationId", message.getConversationId());
        event.put("sequenceNo", String.valueOf(message.getSequenceNo()));
        event.put("role", message.getRole());
        event.put("content", message.getContent() == null ? "" : message.getContent());
        event.put("status", message.getStatus());
        event.put("toolName", nullToEmpty(message.getToolName()));
        event.put("toolArguments", nullToEmpty(message.getToolArguments()));
        event.put("toolResult", nullToEmpty(message.getToolResult()));
        event.put("parentMessageId", nullToEmpty(message.getParentMessageId()));
        event.put("attemptNo", String.valueOf(message.getAttemptNo() == null ? 1 : message.getAttemptNo()));
        event.put("createdAt", message.getCreatedAt().toString());
        MapRecord<String, String, String> record = StreamRecords.newRecord().in(STREAM).ofMap(event);
        redisTemplate.opsForStream().add(record);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
