package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.entity.AiModelConfig;
import com.novelgeneration.novel.mapper.AiModelConfigMapper;
import com.novelgeneration.novel.service.ModelConfigResolver;
import com.novelgeneration.novel.utils.ApiKeyEncryptor;
import com.novelgeneration.novel.vo.AiModelConfigSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ModelConfigResolverImpl implements ModelConfigResolver {
    @Resource
    private AiModelConfigMapper mapper;

    @Resource
    private ApiKeyEncryptor encryptor;

    @Value("${ai.api-key:}")
    private String textApiKey;
    @Value("${ai.api-url:https://api.deepseek.com/chat/completions}")
    private String textApiUrl;
    @Value("${ai.api-model:deepseek-chat}")
    private String textModel;
    @Value("${image.api-key:}")
    private String imageApiKey;
    @Value("${image.api-url:}")
    private String imageApiUrl;
    @Value("${image.api-model:}")
    private String imageModel;
    @Value("${video.generation.enabled:false}")
    private boolean videoEnabled;
    @Value("${video.generation.provider:ark}")
    private String videoProvider;
    @Value("${video.generation.endpoint:}")
    private String videoApiUrl;
    @Value("${video.generation.query-url:}")
    private String videoQueryUrl;
    @Value("${video.generation.api-key:}")
    private String videoApiKey;
    @Value("${video.generation.minimax-api-key:}")
    private String minimaxApiKey;
    @Value("${video.generation.model:}")
    private String videoModel;

    @Override
    public AiModelConfigSnapshot resolveText() {
        return resolve("TEXT", new AiModelConfigSnapshot("TEXT", "OPENAI_COMPATIBLE", textApiUrl,
                null, textApiKey, textModel, !blank(textApiKey), false));
    }

    @Override
    public AiModelConfigSnapshot resolveImage() {
        return resolve("IMAGE", new AiModelConfigSnapshot("IMAGE", "OPENAI_COMPATIBLE", imageApiUrl,
                null, imageApiKey, imageModel, !blank(imageApiKey) && !blank(imageApiUrl) && !blank(imageModel), false));
    }

    @Override
    public AiModelConfigSnapshot resolveVideo() {
        String propertyKey = "minimax".equalsIgnoreCase(videoProvider) ? minimaxApiKey : videoApiKey;
        return resolve("VIDEO", new AiModelConfigSnapshot("VIDEO", videoProvider, videoApiUrl,
                videoQueryUrl, propertyKey, videoModel, videoEnabled, false));
    }

    private AiModelConfigSnapshot resolve(String type, AiModelConfigSnapshot fallback) {
        try {
            AiModelConfig active = mapper.selectActive(type);
            if (active == null) return fallback;
            String apiKey = encryptor.decrypt(active.getEncryptedApiKey());
            String queryUrl = blank(active.getQueryUrl()) ? fallback.getQueryUrl() : active.getQueryUrl();
            return new AiModelConfigSnapshot(type, active.getProviderType(), active.getApiUrl(), queryUrl,
                    apiKey, active.getModelName(), Boolean.TRUE.equals(active.getEnabled()), true);
        } catch (DataAccessException e) {
            // The migration is optional for existing installations; retain properties fallback until it is run.
            return fallback;
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
