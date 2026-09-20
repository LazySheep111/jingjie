package com.novelgeneration.novel.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.novelgeneration.novel.service.ModelConfigResolver;
import com.novelgeneration.novel.vo.AiModelConfigSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiUtilStructuredOutputTest {

    @Test
    void sendsJsonSchemaResponseFormatToOpenAiCompatibleEndpoint() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ModelConfigResolver resolver = mock(ModelConfigResolver.class);
        AiUtil aiUtil = new AiUtil();
        ReflectionTestUtils.setField(aiUtil, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(aiUtil, "modelConfigResolver", resolver);

        when(resolver.resolveText()).thenReturn(new AiModelConfigSnapshot(
                "TEXT", "OPENAI_COMPATIBLE", "https://example.test/chat/completions", null,
                "secret", "deepseek-chat", true, true));
        when(restTemplate.postForObject(eq("https://example.test/chat/completions"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", java.util.List.of(Map.of(
                        "message", Map.of("content", "{\"novelTitle\":\"测试\"}")))));

        ObjectNode schema = new ObjectMapper().createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("novelTitle").put("type", "string");
        schema.putArray("required").add("novelTitle");

        assertEquals("{\"novelTitle\":\"测试\"}", aiUtil.chatStructured("生成大纲", "novel_outline", schema));

        var captor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(eq("https://example.test/chat/completions"), captor.capture(), eq(Map.class));
        Map<?, ?> body = (Map<?, ?>) captor.getValue().getBody();
        Map<?, ?> responseFormat = (Map<?, ?>) body.get("response_format");
        assertEquals("json_schema", responseFormat.get("type"));
        Map<?, ?> jsonSchema = (Map<?, ?>) responseFormat.get("json_schema");
        assertEquals("novel_outline", jsonSchema.get("name"));
        assertNotNull(jsonSchema.get("schema"));
    }

    @Test
    void fallsBackToLegacyJsonPromptWhenProviderRejectsSchema() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ModelConfigResolver resolver = mock(ModelConfigResolver.class);
        AiUtil aiUtil = new AiUtil();
        ReflectionTestUtils.setField(aiUtil, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(aiUtil, "modelConfigResolver", resolver);

        when(resolver.resolveText()).thenReturn(new AiModelConfigSnapshot(
                "TEXT", "OPENAI_COMPATIBLE", "https://example.test/chat/completions", null,
                "secret", "legacy-model", true, true));
        when(restTemplate.postForObject(eq("https://example.test/chat/completions"), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "unsupported", null, null, null))
                .thenReturn(Map.of("choices", java.util.List.of(Map.of(
                        "message", Map.of("content", "{\"ok\":true}")))));

        ObjectNode schema = new ObjectMapper().createObjectNode();
        schema.put("type", "object");

        assertEquals("{\"ok\":true}", aiUtil.chatStructured("生成 JSON", "test_schema", schema));
    }

    @Test
    void sendsDeepSeekJsonObjectResponseFormat() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ModelConfigResolver resolver = mock(ModelConfigResolver.class);
        AiUtil aiUtil = new AiUtil();
        ReflectionTestUtils.setField(aiUtil, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(aiUtil, "modelConfigResolver", resolver);

        when(resolver.resolveText()).thenReturn(new AiModelConfigSnapshot(
                "TEXT", "OPENAI_COMPATIBLE", "https://example.test/chat/completions", null,
                "secret", "deepseek-v4-flash", true, true));
        when(restTemplate.postForObject(eq("https://example.test/chat/completions"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", java.util.List.of(Map.of(
                        "message", Map.of("content", "{\"ok\":true}")))));

        aiUtil.chatJsonObject("生成大纲");

        var captor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(eq("https://example.test/chat/completions"), captor.capture(), eq(Map.class));
        Map<?, ?> body = (Map<?, ?>) captor.getValue().getBody();
        Map<?, ?> responseFormat = (Map<?, ?>) body.get("response_format");
        assertEquals("json_object", responseFormat.get("type"));
    }
}
