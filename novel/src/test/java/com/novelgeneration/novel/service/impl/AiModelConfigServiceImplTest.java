package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.AiModelConfigRequest;
import com.novelgeneration.novel.entity.AiModelConfig;
import com.novelgeneration.novel.mapper.AiModelConfigMapper;
import com.novelgeneration.novel.utils.ApiKeyEncryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiModelConfigServiceImplTest {
    @Mock
    private AiModelConfigMapper mapper;
    @Mock
    private ApiKeyEncryptor encryptor;
    @Mock
    private RestTemplate restTemplate;
    @InjectMocks
    private AiModelConfigServiceImpl service;

    @Test
    void failedTestDoesNotWriteOrDisableExistingConfig() {
        AiModelConfigRequest request = request("sk-new");
        when(restTemplate.postForEntity(anyString(), any(), any())).thenThrow(new RestClientException("timeout"));

        var result = service.test("TEXT", request);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("timeout"));
        verify(mapper, never()).insertVersion(any());
        verify(mapper, never()).disableActive(anyString());
    }

    @Test
    void saveTestsBeforeWritingAndStoresEncryptedKey() {
        AiModelConfigRequest request = request("sk-new");
        when(restTemplate.postForEntity(anyString(), any(), any())).thenReturn(ResponseEntity.ok("{}"));
        when(encryptor.encrypt("sk-new")).thenReturn("ciphertext");
        when(encryptor.decrypt("ciphertext")).thenReturn("sk-new");
        when(encryptor.mask("sk-new")).thenReturn("sk-****new");
        when(mapper.selectLatestVersion("TEXT")).thenReturn(2);

        var result = service.save("TEXT", request);

        ArgumentCaptor<AiModelConfig> captor = ArgumentCaptor.forClass(AiModelConfig.class);
        verify(mapper).disableActive("TEXT");
        verify(mapper).insertVersion(captor.capture());
        assertEquals("ciphertext", captor.getValue().getEncryptedApiKey());
        assertEquals(3, captor.getValue().getVersion());
        assertTrue(captor.getValue().getEnabled());
        assertEquals("sk-****new", result.getApiKeyMasked());
    }

    @Test
    void listReturnsFixedThreeCapabilitySlotsWithoutRawKey() {
        when(mapper.selectActive(anyString())).thenReturn(null);

        var result = service.list();

        assertEquals(List.of("TEXT", "IMAGE", "VIDEO"), result.stream().map(v -> v.getCapabilityType()).toList());
        assertEquals("未配置", result.get(0).getApiKeyMasked());
        assertFalse(result.get(0).getEnabled());
    }

    @Test
    void imageReachabilityUsesValidationPostForPostOnlyImageEndpoint() {
        AiModelConfigRequest request = request("ark-key");
        request.setApiUrl("https://ark.cn-beijing.volces.com/api/v3/images/generations");
        request.setModelName("seedream-test");
        when(restTemplate.postForEntity(anyString(), any(), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("prompt is required"));

        var result = service.test("IMAGE", request);

        assertTrue(result.isSuccess());
        verify(restTemplate).postForEntity(anyString(), any(), any());
    }

    @Test
    void videoQueryUrlAllowsTaskIdPlaceholder() {
        AiModelConfigRequest request = request("minimax-key");
        request.setProviderType("MINIMAX");
        request.setApiUrl("https://api.minimaxi.com/v2/video_generation");
        request.setQueryUrl("https://api.minimaxi.com/v2/query/video_generation/{taskId}");
        request.setModelName("MiniMax-H3");
        when(restTemplate.postForEntity(anyString(), any(), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("content is required"));

        var result = service.test("VIDEO", request);

        assertTrue(result.isSuccess());
    }

    private AiModelConfigRequest request(String apiKey) {
        AiModelConfigRequest request = new AiModelConfigRequest();
        request.setProviderType("OPENAI_COMPATIBLE");
        request.setApiUrl("https://api.example.com/v1/chat/completions");
        request.setApiKey(apiKey);
        request.setModelName("test-model");
        return request;
    }
}
