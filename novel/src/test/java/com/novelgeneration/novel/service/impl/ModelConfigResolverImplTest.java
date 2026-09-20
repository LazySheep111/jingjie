package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.entity.AiModelConfig;
import com.novelgeneration.novel.mapper.AiModelConfigMapper;
import com.novelgeneration.novel.utils.ApiKeyEncryptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelConfigResolverImplTest {
    @Mock
    private AiModelConfigMapper mapper;
    @Mock
    private ApiKeyEncryptor encryptor;
    @InjectMocks
    private ModelConfigResolverImpl resolver;

    @Test
    void activeDatabaseConfigOverridesProperties() {
        AiModelConfig active = new AiModelConfig();
        active.setCapabilityType("TEXT");
        active.setProviderType("OPENAI_COMPATIBLE");
        active.setApiUrl("https://db.example.com/chat");
        active.setEncryptedApiKey("cipher");
        active.setModelName("db-model");
        active.setEnabled(true);
        when(mapper.selectActive("TEXT")).thenReturn(active);
        when(encryptor.decrypt("cipher")).thenReturn("db-secret");

        var result = resolver.resolveText();

        assertEquals("https://db.example.com/chat", result.getApiUrl());
        assertEquals("db-secret", result.getApiKey());
        assertEquals("db-model", result.getModelName());
        assertTrue(result.isDatabaseSource());
    }

    @Test
    void missingDatabaseConfigKeepsExistingPropertyFallback() {
        when(mapper.selectActive("IMAGE")).thenReturn(null);
        ReflectionTestUtils.setField(resolver, "imageApiKey", "property-key");
        ReflectionTestUtils.setField(resolver, "imageApiUrl", "https://property.example.com/images");
        ReflectionTestUtils.setField(resolver, "imageModel", "property-model");

        var result = resolver.resolveImage();

        assertEquals("property-key", result.getApiKey());
        assertEquals("https://property.example.com/images", result.getApiUrl());
        assertEquals("property-model", result.getModelName());
        assertFalse(result.isDatabaseSource());
    }
}
