package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.mapper.AssistantConversationMapper;
import com.novelgeneration.novel.mapper.AssistantMessageMapper;
import com.novelgeneration.novel.service.AssistantConversationService;
import com.novelgeneration.novel.service.AssistantMessageEventPublisher;
import com.novelgeneration.novel.vo.AssistantConversationVO;
import com.novelgeneration.novel.vo.AssistantMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AssistantConversationServiceImpl implements AssistantConversationService {
    private static final long CACHE_SECONDS = 3600L;
    private final RedisTemplate<String, String> redisTemplate;
    private final AssistantConversationMapper conversationMapper;
    private final AssistantMessageMapper messageMapper;
    private final ObjectMapper objectMapper;
    private final AssistantMessageEventPublisher eventPublisher;

    @Override
    public AssistantConversationVO getOrCreate(Long novelId, String title) {
        long normalizedNovelId = novelId == null ? 0L : novelId;
        AssistantConversationVO conversation = conversationMapper.selectByNovelId(normalizedNovelId);
        if (conversation == null) {
            conversation = new AssistantConversationVO();
            conversation.setConversationId("conv-" + normalizedNovelId + "-" + UUID.randomUUID());
            conversation.setNovelId(normalizedNovelId);
            conversation.setTitle(title == null || title.isBlank() ? "通用助手" : title);
            conversation.setConversationVersion(1L);
            conversationMapper.insert(conversation);
        }
        try {
            cacheConversation(conversation);
            refreshTtl(conversation);
        } catch (RuntimeException e) {
            // Redis is an accelerator; the database remains the source of truth.
        }
        return conversation;
    }

    @Override
    public List<AssistantMessageVO> recentMessages(AssistantConversationVO conversation, int rounds) {
        if (conversation == null) return Collections.emptyList();
        String key = messageKey(conversation.getConversationId());
        List<String> cached;
        try { cached = redisTemplate.opsForList().range(key, 0, Math.max(1, rounds * 2) - 1); }
        catch (RuntimeException e) { cached = Collections.emptyList(); }
        List<AssistantMessageVO> messages = new ArrayList<>();
        if (cached != null) {
            for (String item : cached) {
                try { messages.add(objectMapper.readValue(item, AssistantMessageVO.class)); }
                catch (JsonProcessingException e) { throw new IllegalStateException("读取 AI 聊天缓存失败", e); }
            }
        }
        if (messages.isEmpty()) {
            messages = messageMapper.selectRecent(conversation.getConversationId(), Math.max(1, rounds * 2));
            Collections.reverse(messages);
            for (AssistantMessageVO message : messages) {
                try { cacheMessage(message); } catch (RuntimeException ignored) { }
            }
        }
        try { refreshTtl(conversation); } catch (RuntimeException ignored) { }
        return messages;
    }

    @Override
    public AssistantMessageVO record(String conversationId, Long novelId, String role, String content,
                                     String status, String toolName, String toolArguments, String toolResult,
                                     String parentMessageId, int attemptNo) {
        AssistantMessageVO message = new AssistantMessageVO();
        message.setMessageId("msg-" + UUID.randomUUID());
        message.setConversationId(conversationId);
        message.setSequenceNo(messageMapper.nextSequence(conversationId));
        message.setRole(role);
        message.setContent(content == null ? "" : content);
        message.setStatus(status == null ? "COMPLETED" : status);
        message.setToolName(toolName);
        message.setToolArguments(toolArguments);
        message.setToolResult(toolResult);
        message.setParentMessageId(parentMessageId);
        message.setAttemptNo(attemptNo <= 0 ? 1 : attemptNo);
        message.setCreatedAt(LocalDateTime.now());
        try { cacheMessage(message); } catch (RuntimeException ignored) { }
        try { refreshTtlByIds(novelId, conversationId); } catch (RuntimeException ignored) { }
        try {
            eventPublisher.publish(message);
        } catch (RuntimeException redisUnavailable) {
            persistDirectly(message);
        }
        return message;
    }

    @Override
    public List<AssistantConversationVO> list(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        return conversationMapper.selectPage(Math.max(page, 0) * safeSize, safeSize);
    }

    @Override
    public long count() { return conversationMapper.countAll(); }

    @Override
    public void delete(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) return;
        // 删除必须先写入 7 天标记，避免 Redis 短暂不可用时，旧 Stream 事件把会话重新写回来。
        redisTemplate.opsForValue().set("assistant:conversation:deleted:" + conversationId, "1", Duration.ofDays(7));
        try { redisTemplate.delete(messageKey(conversationId)); } catch (RuntimeException ignored) { }
        conversationMapper.deleteByConversationId(conversationId);
        messageMapper.deleteByConversationId(conversationId);
    }

    private void cacheConversation(AssistantConversationVO conversation) {
        try {
            redisTemplate.opsForValue().set(conversationKey(conversation.getNovelId()),
                    objectMapper.writeValueAsString(conversation), Duration.ofSeconds(CACHE_SECONDS));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("缓存 AI 会话失败", e);
        }
    }

    private void cacheMessage(AssistantMessageVO message) {
        try {
            String key = messageKey(message.getConversationId());
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(message));
            redisTemplate.opsForList().trim(key, -40, -1);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("缓存 AI 消息失败", e);
        }
    }

    private void persistDirectly(AssistantMessageVO message) {
        try {
            messageMapper.insert(message);
            AssistantConversationVO summary = new AssistantConversationVO();
            summary.setConversationId(message.getConversationId());
            summary.setLastMessagePreview(message.getContent() == null ? "" : message.getContent());
            summary.setMessageCount(messageMapper.countByConversationId(message.getConversationId()));
            conversationMapper.updateSummary(summary);
        } catch (org.springframework.dao.DuplicateKeyException ignored) {
            // A concurrent Stream consumer may already have persisted the message.
        }
    }

    private void refreshTtl(AssistantConversationVO conversation) {
        refreshTtlByIds(conversation.getNovelId(), conversation.getConversationId());
    }

    private void refreshTtlByIds(Long novelId, String conversationId) {
        if (novelId != null) redisTemplate.expire(conversationKey(novelId), CACHE_SECONDS, TimeUnit.SECONDS);
        if (conversationId != null) redisTemplate.expire(messageKey(conversationId), CACHE_SECONDS, TimeUnit.SECONDS);
    }

    private String conversationKey(Long novelId) {
        return "assistant:conversation:" + (novelId == null ? 0 : novelId);
    }

    private String messageKey(String conversationId) {
        return "assistant:messages:" + conversationId;
    }
}
