package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.mapper.AssistantConversationMapper;
import com.novelgeneration.novel.mapper.AssistantMessageMapper;
import com.novelgeneration.novel.vo.AssistantConversationVO;
import com.novelgeneration.novel.vo.AssistantMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssistantMessagePersistenceConsumer {
    private static final String STREAM = AssistantMessageEventPublisherImpl.STREAM;
    private static final String GROUP = "assistant-message-persistence";
    private static final String CONSUMER = "assistant-persistence-worker";
    private final RedisTemplate<String, String> redisTemplate;
    private final AssistantConversationMapper conversationMapper;
    private final AssistantMessageMapper messageMapper;

    @PostConstruct
    public void ensureConsumerGroup() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM, ReadOffset.from("0-0"), GROUP);
        } catch (Exception ignored) {
            // Stream/group already exists, or Redis is unavailable and will be retried later.
        }
    }

    @Scheduled(fixedDelayString = "${assistant.persistence.poll-ms:1000}")
    public void consume() {
        try {
            Consumer consumer = Consumer.from(GROUP, CONSUMER);
            StreamReadOptions options = StreamReadOptions.empty().count(20).block(Duration.ofMillis(100));
            // 先重试当前消费者遗留的 pending 消息，再读取新消息。
            List<MapRecord<String, Object, Object>> pending = redisTemplate.opsForStream().read(
                    consumer, options, StreamOffset.create(STREAM, ReadOffset.from("0-0")));
            if (pending != null) {
                for (MapRecord<String, Object, Object> record : pending) persist(record);
            }
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                    consumer, options, StreamOffset.create(STREAM, ReadOffset.lastConsumed()));
            if (records != null) {
                for (MapRecord<String, Object, Object> record : records) persist(record);
            }
        } catch (Exception e) {
            log.warn("AI 聊天持久化消费失败: {}", e.getMessage());
        }
    }

    private void persist(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();
        String conversationId = stringValue(value.get("conversationId"));
        String deletedKey = "assistant:conversation:deleted:" + conversationId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(deletedKey))) {
            redisTemplate.opsForStream().acknowledge(STREAM, GROUP, record.getId());
            return;
        }
        AssistantMessageVO message = new AssistantMessageVO();
        message.setMessageId(stringValue(value.get("messageId")));
        message.setConversationId(conversationId);
        message.setSequenceNo(Long.valueOf(stringValue(value.get("sequenceNo"))));
        message.setRole(stringValue(value.get("role")));
        message.setContent(stringValue(value.get("content")));
        message.setStatus(stringValue(value.get("status")));
        message.setToolName(emptyToNull(stringValue(value.get("toolName"))));
        message.setToolArguments(emptyToNull(stringValue(value.get("toolArguments"))));
        message.setToolResult(emptyToNull(stringValue(value.get("toolResult"))));
        message.setParentMessageId(emptyToNull(stringValue(value.get("parentMessageId"))));
        message.setAttemptNo(Integer.valueOf(stringValue(value.get("attemptNo"))));
        message.setCreatedAt(LocalDateTime.parse(stringValue(value.get("createdAt"))));
        try {
            messageMapper.insert(message);
            updateSummary(conversationId, message);
            redisTemplate.opsForStream().acknowledge(STREAM, GROUP, record.getId());
        } catch (DuplicateKeyException duplicate) {
            // The previous attempt may have inserted the message before failing to update the summary.
            updateSummary(conversationId, message);
            redisTemplate.opsForStream().acknowledge(STREAM, GROUP, record.getId());
        } catch (Exception e) {
            log.warn("AI 聊天消息入库失败 messageId={}: {}", value.get("messageId"), e.getMessage());
        }
    }

    private String emptyToNull(String value) { return value == null || value.isBlank() ? null : value; }

    private void updateSummary(String conversationId, AssistantMessageVO message) {
        AssistantConversationVO conversation = new AssistantConversationVO();
        conversation.setConversationId(conversationId);
        conversation.setLastMessagePreview(preview(message.getContent()));
        conversation.setMessageCount(messageMapper.countByConversationId(conversationId));
        conversationMapper.updateSummary(conversation);
    }

    private String stringValue(Object value) { return value == null ? "" : String.valueOf(value); }

    private String preview(String content) {
        if (content == null) return "";
        return content.length() <= 120 ? content : content.substring(0, 120);
    }
}
