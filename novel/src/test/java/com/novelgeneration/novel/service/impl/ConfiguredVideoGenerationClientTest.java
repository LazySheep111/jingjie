package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.dto.VideoProviderRequest;
import com.novelgeneration.novel.service.VideoGenerationClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfiguredVideoGenerationClientTest {
    @Test
    void submitUsesMiniMaxH3V2FirstFrameShape() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"task_id\":\"h3-task\"}"));

        ConfiguredVideoGenerationClient client = configuredClient(restTemplate, "minimax", "MiniMax-H3");
        VideoProviderRequest request = new VideoProviderRequest();
        request.setPrompt("人物向前走");
        request.setStylePrompt("东方玄幻写实");
        request.setDurationSec(10);
        request.setAspectRatio("16:9");
        request.setFirstFrameImageUrl("https://example.com/frame.png");
        request.setResolution("768P");

        VideoGenerationClient.ProviderTask task = client.submit(request);

        assertEquals("h3-task", task.getTaskId());
        org.mockito.ArgumentCaptor<HttpEntity> captor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).postForEntity(eq("https://api.minimaxi.com/v2/video_generation"), captor.capture(), eq(String.class));
        Map<?, ?> body = new ObjectMapper().readValue(String.valueOf(captor.getValue().getBody()), Map.class);
        assertEquals("MiniMax-H3", body.get("model"));
        assertEquals("768P", body.get("resolution"));
        assertEquals("768P", request.getResolution());
        assertEquals("adaptive", body.get("ratio"));
        assertEquals(10, body.get("duration"));
        assertFalse(body.containsKey("generate_audio"));
        assertFalse(body.containsKey("watermark"));
        assertTrue(String.valueOf(body.get("content")).contains("first_frame"));
        assertEquals("Bearer minimax-key", captor.getValue().getHeaders().getFirst("Authorization"));
    }

    @Test
    void unsupportedMiniMaxH3ResolutionFallsBackTo768P() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"task_id\":\"h3-task\"}"));

        ConfiguredVideoGenerationClient client = configuredClient(restTemplate, "minimax", "MiniMax-H3");
        VideoProviderRequest request = new VideoProviderRequest();
        request.setPrompt("人物向前走");
        request.setDurationSec(6);
        request.setResolution("1080P");
        request.setFirstFrameImageUrl("https://example.com/frame.png");

        client.submit(request);

        org.mockito.ArgumentCaptor<HttpEntity> captor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).postForEntity(eq("https://api.minimaxi.com/v2/video_generation"), captor.capture(), eq(String.class));
        Map<?, ?> body = new ObjectMapper().readValue(String.valueOf(captor.getValue().getBody()), Map.class);
        assertEquals("768P", body.get("resolution"));
    }

    @Test
    void queryParsesMiniMaxH3NestedTaskResult() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(eq("https://api.minimaxi.com/v2/query/video_generation/h3-task"),
                eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"task\":{\"status\":\"succeeded\",\"content\":{\"url\":\"https://cdn.test/video.mp4\"}}}"));
        ConfiguredVideoGenerationClient client = configuredClient(restTemplate, "minimax", "MiniMax-H3");

        VideoGenerationClient.ProviderTask task = client.query("h3-task");

        assertEquals("h3-task", task.getTaskId());
        assertEquals("SUCCESS", task.getStatus());
        assertEquals("https://cdn.test/video.mp4", task.getVideoUrl());
    }

    @Test
    void submitUsesArkModelAndFirstFrameRequestShape() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":\"task-1\"}"));

        ConfiguredVideoGenerationClient client = new ConfiguredVideoGenerationClient();
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(client, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(client, "enabled", true);
        ReflectionTestUtils.setField(client, "endpoint", "https://ark.cn-beijing.volces.com/api/v3/contents/generations/tasks");
        ReflectionTestUtils.setField(client, "queryUrl", "https://ark.cn-beijing.volces.com/api/v3/contents/generations/tasks/{taskId}");
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        ReflectionTestUtils.setField(client, "model", "doubao-seedance-2-0-260128");

        VideoProviderRequest request = new VideoProviderRequest();
        request.setPrompt("河边少年向前走");
        request.setDurationSec(10);
        request.setFirstFrameImageUrl("https://example.com/frame.png");

        VideoGenerationClient.ProviderTask task = client.submit(request);

        assertEquals("task-1", task.getTaskId());
        org.mockito.ArgumentCaptor<HttpEntity> captor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).postForEntity(eq("https://ark.cn-beijing.volces.com/api/v3/contents/generations/tasks"), captor.capture(), eq(String.class));
        HttpEntity<?> entity = captor.getValue();
        String body = String.valueOf(entity.getBody());
        assertTrue(body.contains("\"model\":\"doubao-seedance-2-0-260128\""));
        assertTrue(body.contains("\"type\":\"image_url\""));
        assertTrue(body.contains("\"role\":\"first_frame\""));
        assertTrue(body.contains("https://example.com/frame.png"));
        assertTrue(body.contains("\"generate_audio\":true"));
        assertFalse(body.contains("reference_image"));
        assertEquals("Bearer test-key", entity.getHeaders().getFirst("Authorization"));
    }

    @Test
    void seed3dModelOmitsUnsupportedRatio() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":\"task-3d\"}"));

        ConfiguredVideoGenerationClient client = new ConfiguredVideoGenerationClient();
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(client, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(client, "enabled", true);
        ReflectionTestUtils.setField(client, "endpoint", "https://example.com/tasks");
        ReflectionTestUtils.setField(client, "queryUrl", "https://example.com/tasks/{taskId}");
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        ReflectionTestUtils.setField(client, "model", "doubao-seed3d-2-0-260328");

        VideoProviderRequest request = new VideoProviderRequest();
        request.setPrompt("三维场景动画");
        request.setAspectRatio("16:9");
        request.setDurationSec(10);
        request.setFirstFrameImageUrl("https://example.com/frame.png");

        client.submit(request);

        org.mockito.ArgumentCaptor<HttpEntity> captor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).postForEntity(eq("https://example.com/tasks"), captor.capture(), eq(String.class));
        Map<?, ?> body = new ObjectMapper().readValue(String.valueOf(captor.getValue().getBody()), Map.class);
        assertTrue(!body.containsKey("ratio"));
        assertTrue(!body.containsKey("duration"));
    }

    private ConfiguredVideoGenerationClient configuredClient(RestTemplate restTemplate, String provider, String model) {
        ConfiguredVideoGenerationClient client = new ConfiguredVideoGenerationClient();
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(client, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(client, "enabled", true);
        ReflectionTestUtils.setField(client, "provider", provider);
        ReflectionTestUtils.setField(client, "endpoint", "https://api.minimaxi.com/v2/video_generation");
        ReflectionTestUtils.setField(client, "queryUrl", "https://api.minimaxi.com/v2/query/video_generation/{taskId}");
        ReflectionTestUtils.setField(client, "apiKey", "ark-key");
        ReflectionTestUtils.setField(client, "minimaxApiKey", "minimax-key");
        ReflectionTestUtils.setField(client, "model", model);
        return client;
    }
}
