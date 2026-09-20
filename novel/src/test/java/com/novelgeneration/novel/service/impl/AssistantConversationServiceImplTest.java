package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.mapper.AssistantConversationMapper;
import com.novelgeneration.novel.mapper.AssistantMessageMapper;
import com.novelgeneration.novel.service.AssistantMessageEventPublisher;
import com.novelgeneration.novel.vo.AssistantConversationVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantConversationServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private AssistantConversationMapper conversationMapper;
    @Mock
    private AssistantMessageMapper messageMapper;
    @Mock
    private AssistantMessageEventPublisher eventPublisher;

    @Test
    void usesOneConversationPerNovelAndRefreshesTheOneHourCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        AssistantConversationVO existing = new AssistantConversationVO();
        existing.setConversationId("conv-54");
        existing.setNovelId(54L);
        when(conversationMapper.selectByNovelId(54L)).thenReturn(existing);

        AssistantConversationServiceImpl service = new AssistantConversationServiceImpl(
                redisTemplate, conversationMapper, messageMapper, new ObjectMapper(), eventPublisher);

        AssistantConversationVO result = service.getOrCreate(54L, "小说 54");

        assertEquals("conv-54", result.getConversationId());
        verify(redisTemplate).expire(eq("assistant:conversation:54"), eq(3600L), any());
        verify(redisTemplate).expire(eq("assistant:messages:conv-54"), eq(3600L), any());
    }
}
