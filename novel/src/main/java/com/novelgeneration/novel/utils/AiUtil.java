package com.novelgeneration.novel.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.service.ModelConfigResolver;
import com.novelgeneration.novel.vo.AiModelConfigSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AiUtil {

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.api-url:https://api.deepseek.com}")
    private String apiUrl;

    @Value("${ai.api-model:deepseek-chat}")
    private String apiModel;

    @Value("${ai.structured-output.enabled:true}")
    private boolean structuredOutputEnabled = true;

    @Resource(name = "aiRestTemplate")
    private RestTemplate restTemplate;

    @Resource
    private ModelConfigResolver modelConfigResolver;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String chat(String prompt) {
        AiModelConfigSnapshot config = resolveConfig();
        if (config == null || config.getApiKey() == null || config.getApiKey().isBlank()
                || "your-key".equals(config.getApiKey())) {
            throw new IllegalStateException("Please configure ai.api-key in application.properties");
        }

        return request(config, prompt, null);
    }

    public String chatStructured(String prompt, String schemaName, JsonNode schema) {
        if (!structuredOutputEnabled) {
            return chat(prompt);
        }

        AiModelConfigSnapshot config = requireTextConfig();
        Map<String, Object> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_schema");
        Map<String, Object> jsonSchema = new HashMap<>();
        jsonSchema.put("name", schemaName);
        jsonSchema.put("strict", true);
        jsonSchema.put("schema", objectMapper.convertValue(schema, Map.class));
        responseFormat.put("json_schema", jsonSchema);

        try {
            log.info("AI structured output request: schema={}, model={}", schemaName, config.getModelName());
            return request(config, prompt, responseFormat);
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            if (status != 400 && status != 404 && status != 422) {
                throw e;
            }
            // Some OpenAI-compatible providers only support the older JSON mode.
            log.warn("AI model rejected JSON Schema: schema={}, model={}, status={}; falling back to legacy JSON mode",
                    schemaName, config.getModelName(), status);
            return chat(prompt);
        }
    }

    public String chatJsonObject(String prompt) {
        AiModelConfigSnapshot config = requireTextConfig();
        Map<String, Object> responseFormat = Map.of("type", "json_object");
        try {
            log.info("AI JSON object output request: model={}", config.getModelName());
            return request(config, prompt, responseFormat);
        } catch (HttpStatusCodeException e) {
            log.warn("AI model rejected JSON object output: model={}, status={}; falling back to legacy JSON mode",
                    config.getModelName(), e.getStatusCode().value());
            return chat(prompt);
        }
    }

    private String request(AiModelConfigSnapshot config, String prompt, Map<String, Object> responseFormat) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + config.getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModelName());
        body.put("temperature", 0.7);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        if (responseFormat != null) {
            body.put("response_format", responseFormat);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> result = restTemplate.postForObject(config.getApiUrl(), request, Map.class);
        if (result == null || result.get("choices") == null) {
            throw new IllegalStateException("AI response is empty");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
        if (choices.isEmpty()) {
            throw new IllegalStateException("AI response has no choices");
        }

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        Object content = message == null ? null : message.get("content");
        if (!(content instanceof String)) {
            throw new IllegalStateException("AI response content is empty");
        }
        return (String) content;
    }

    private AiModelConfigSnapshot requireTextConfig() {
        AiModelConfigSnapshot config = resolveConfig();
        if (config == null || config.getApiKey() == null || config.getApiKey().isBlank()
                || "your-key".equals(config.getApiKey())) {
            throw new IllegalStateException("Please configure ai.api-key in application.properties");
        }
        return config;
    }

    private AiModelConfigSnapshot resolveConfig() {
        if (modelConfigResolver != null) {
            return modelConfigResolver.resolveText();
        }
        return new AiModelConfigSnapshot("TEXT", "OPENAI_COMPATIBLE", apiUrl, null,
                apiKey, apiModel, true, false);
    }
}
