package com.novelgeneration.novel.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiUtil {

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.api-url:https://api.deepseek.com}")
    private String apiUrl;

    @Value("${ai.api-model:deepseek-chat}")
    private String apiModel;

    @Resource
    private RestTemplate restTemplate;

    public String chat(String prompt) {
        if (apiKey == null || apiKey.isBlank() || "your-key".equals(apiKey)) {
            throw new IllegalStateException("Please configure ai.api-key in application.properties");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", apiModel);
        body.put("temperature", 0.7);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        Map<String, Object> result = restTemplate.postForObject(apiUrl, request, Map.class);
        if (result == null || result.get("choices") == null) {
            throw new IllegalStateException("AI response is empty");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
        if (choices.isEmpty()) {
            throw new IllegalStateException("AI response has no choices");
        }

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
