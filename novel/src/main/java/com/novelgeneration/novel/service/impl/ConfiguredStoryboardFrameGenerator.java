package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.dto.StoryboardFrameRequest;
import com.novelgeneration.novel.service.ModelConfigResolver;
import com.novelgeneration.novel.service.StoryboardFrameGenerator;
import com.novelgeneration.novel.vo.AiModelConfigSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ConfiguredStoryboardFrameGenerator implements StoryboardFrameGenerator {
    @Resource
    private RestTemplate restTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ModelConfigResolver modelConfigResolver;

    @Value("${image.api-key:}")
    private String apiKey;

    @Value("${image.api-url:}")
    private String apiUrl;

    @Value("${image.api-model:}")
    private String apiModel;

    @Value("${image.storyboard-frame-dir:uploads/storyboard-frames}")
    private String frameDir;

    @Override
    public String generate(StoryboardFrameRequest request) {
        validate(request);
        AiModelConfigSnapshot config = resolveImageConfig();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModelName());
            body.put("prompt", request.getPrompt());
            body.put("image", request.getReferenceImageUrls());
            body.put("size", "2K");
            body.put("sequential_image_generation", "disabled");
            body.put("response_format", "b64_json");
            body.put("stream", false);
            body.put("watermark", false);

            ResponseEntity<String> response = restTemplate.postForEntity(resolveGenerationUrl(config.getApiUrl()),
                    new HttpEntity<>(body, headers), String.class);
            byte[] bytes = readImageBytes(response.getBody());
            Path directory = Paths.get(frameDir, String.valueOf(request.getNovelId()),
                    "chapter-" + request.getChapterNum(), "scene-" + request.getSceneId())
                    .toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String filename = "frame-" + UUID.randomUUID() + ".png";
            Files.write(directory.resolve(filename), bytes);
            return "/api/storyboard-frames/files/" + request.getNovelId() + "/chapter-"
                    + request.getChapterNum() + "/scene-" + request.getSceneId() + "/" + filename;
        } catch (HttpStatusCodeException e) {
            String detail = blank(e.getResponseBodyAsString()) ? e.getStatusText() : e.getResponseBodyAsString();
            throw new IllegalStateException("分镜首帧生成失败：" + detail, e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException("分镜首帧生成失败", e);
        }
    }

    private byte[] readImageBytes(String responseBody) throws Exception {
        if (blank(responseBody)) {
            throw new IllegalStateException("图片接口返回为空");
        }
        JsonNode image = objectMapper.readTree(responseBody).path("data").path(0);
        String encoded = image.path("b64_json").asText(null);
        if (!blank(encoded)) {
            return Base64.getDecoder().decode(encoded);
        }
        String remoteUrl = image.path("url").asText(null);
        if (blank(remoteUrl)) {
            throw new IllegalStateException("图片接口未返回 data[0].b64_json 或 data[0].url");
        }
        ResponseEntity<byte[]> response = restTemplate.getForEntity(remoteUrl, byte[].class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().length == 0) {
            throw new IllegalStateException("分镜首帧下载失败");
        }
        return response.getBody();
    }

    private String resolveGenerationUrl(String configuredUrl) {
        String normalized = configuredUrl.endsWith("/") ? configuredUrl.substring(0, configuredUrl.length() - 1) : configuredUrl;
        return normalized.endsWith("/images/generations") ? normalized : normalized + "/images/generations";
    }

    private AiModelConfigSnapshot resolveImageConfig() {
        if (modelConfigResolver != null) {
            return modelConfigResolver.resolveImage();
        }
        return new AiModelConfigSnapshot("IMAGE", "OPENAI_COMPATIBLE", apiUrl, null,
                apiKey, apiModel, true, false);
    }

    private void validate(StoryboardFrameRequest request) {
        if (request == null || request.getNovelId() == null || request.getChapterNum() == null
                || request.getSceneId() == null || blank(request.getPrompt())) {
            throw new IllegalArgumentException("分镜首帧生成参数不完整");
        }
        if (request.getReferenceImageUrls() == null || request.getReferenceImageUrls().isEmpty()) {
            throw new IllegalArgumentException("分镜首帧缺少人物或场景参考图");
        }
        AiModelConfigSnapshot config = resolveImageConfig();
        if (config == null || !config.isEnabled() || blank(config.getApiKey()) || blank(config.getApiUrl())
                || blank(config.getModelName())) {
            throw new IllegalStateException("请配置 image.api-key、image.api-url 和 image.api-model");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
