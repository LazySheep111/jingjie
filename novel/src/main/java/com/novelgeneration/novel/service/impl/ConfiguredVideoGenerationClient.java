package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.dto.VideoProviderRequest;
import com.novelgeneration.novel.service.ModelConfigResolver;
import com.novelgeneration.novel.service.VideoGenerationClient;
import com.novelgeneration.novel.vo.AiModelConfigSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ConfiguredVideoGenerationClient implements VideoGenerationClient {
    @Resource
    private RestTemplate restTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ModelConfigResolver modelConfigResolver;

    @Value("${video.generation.enabled:false}")
    private boolean enabled;

    @Value("${video.generation.provider:ark}")
    private String provider;

    @Value("${video.generation.endpoint:}")
    private String endpoint;

    @Value("${video.generation.query-url:}")
    private String queryUrl;

    @Value("${video.generation.api-key:}")
    private String apiKey;

    @Value("${video.generation.minimax-api-key:}")
    private String minimaxApiKey;

    @Value("${video.generation.model:}")
    private String model;

    @Override
    public ProviderTask submit(VideoProviderRequest request) {
        AiModelConfigSnapshot config = resolveVideoConfig();
        ensureConfigured(config);
        if (request == null || blank(request.getFirstFrameImageUrl())) {
            throw new IllegalArgumentException("视频生成缺少分镜首帧");
        }
        return parseProviderTask(post(config.getApiUrl(), isMiniMax(config) ? miniMaxBody(request, config) : arkBody(request, config), config).getBody());
    }

    private Map<String, Object> arkBody(VideoProviderRequest request, AiModelConfigSnapshot config) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModelName());
        body.put("content", content(request));
        // Seed3D models reject ratio and duration; regular video models accept them.
        if (!isSeed3dModel(config)) {
            body.put("ratio", request.getAspectRatio());
            body.put("duration", request.getDurationSec());
        }
        body.put("generate_audio", true);
        body.put("watermark", false);
        return body;
    }

    private Map<String, Object> miniMaxBody(VideoProviderRequest request, AiModelConfigSnapshot config) {
        int duration = request.getDurationSec();
        if (duration < 4 || duration > 15) {
            throw new IllegalArgumentException("MiniMax-H3 视频时长必须在 4 到 15 秒之间");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModelName());
        body.put("content", content(request));
        String resolution = supportedResolution(request.getResolution(), config);
        request.setResolution(resolution);
        body.put("resolution", resolution);
        body.put("duration", duration);
        body.put("ratio", "adaptive");
        return body;
    }

    private List<Map<String, Object>> content(VideoProviderRequest request) {
        List<Map<String, Object>> content = new ArrayList<>();
        Map<String, Object> text = new HashMap<>();
        text.put("type", "text");
        text.put("text", joinPrompt(request));
        content.add(text);
        Map<String, Object> image = new HashMap<>();
        image.put("type", "image_url");
        image.put("role", "first_frame");
        image.put("image_url", Map.of("url", request.getFirstFrameImageUrl()));
        content.add(image);
        return content;
    }

    @Override
    public ProviderTask query(String providerTaskId) {
        AiModelConfigSnapshot config = resolveVideoConfig();
        ensureConfigured(config);
        String url = config.getQueryUrl().replace("{taskId}", providerTaskId);
        HttpHeaders headers = headers(config);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        return parseProviderTask(response.getBody(), providerTaskId);
    }

    private ResponseEntity<String> post(String url, Map<String, Object> body, AiModelConfigSnapshot config) {
        try {
            return restTemplate.postForEntity(url, new HttpEntity<>(objectMapper.writeValueAsString(body), headers(config)), String.class);
        } catch (HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            String detail = blank(responseBody) ? e.getStatusText() : responseBody;
            throw new IllegalStateException(providerName(config) + "视频任务提交失败：" + detail, e);
        } catch (Exception e) {
            throw new IllegalStateException(providerName(config) + "视频任务提交失败", e);
        }
    }

    private HttpHeaders headers(AiModelConfigSnapshot config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(effectiveApiKey(config));
        return headers;
    }

    private ProviderTask parseProviderTask(String body) {
        return parseProviderTask(body, null);
    }

    private ProviderTask parseProviderTask(String body, String fallbackTaskId) {
        if (blank(body)) {
            throw new IllegalStateException("视频模型返回为空");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            ProviderTask task = new ProviderTask();
            task.setTaskId(firstText(root, "id", "task_id", "request_id"));
            task.setStatus(firstText(root, "status", "state"));
            task.setVideoUrl(firstText(root, "video_url", "url"));
            task.setErrorMessage(firstText(root, "error", "message"));
            JsonNode content = root.path("content");
            if (content.isObject()) {
                if (blank(task.getVideoUrl())) task.setVideoUrl(firstText(content, "video_url", "url"));
                if (blank(task.getStatus())) task.setStatus(firstText(content, "status", "state"));
            } else if (content.isArray()) {
                for (JsonNode item : content) {
                    if (blank(task.getVideoUrl())) task.setVideoUrl(firstText(item, "video_url", "url"));
                    if (blank(task.getStatus())) task.setStatus(firstText(item, "status", "state"));
                }
            }
            JsonNode data = root.path("data");
            if (data.isObject()) {
                if (blank(task.getTaskId())) task.setTaskId(firstText(data, "id", "task_id"));
                if (blank(task.getStatus())) task.setStatus(firstText(data, "status", "state"));
                if (blank(task.getVideoUrl())) task.setVideoUrl(firstText(data, "video_url", "url"));
            }
            JsonNode nestedTask = root.path("task");
            if (nestedTask.isObject()) {
                if (blank(task.getTaskId())) task.setTaskId(firstText(nestedTask, "id", "task_id"));
                if (blank(task.getStatus())) task.setStatus(firstText(nestedTask, "status", "state"));
                if (blank(task.getVideoUrl())) task.setVideoUrl(firstText(nestedTask, "video_url", "url"));
                JsonNode nestedContent = nestedTask.path("content");
                if (nestedContent.isObject() && blank(task.getVideoUrl())) {
                    task.setVideoUrl(firstText(nestedContent, "video_url", "url"));
                }
                JsonNode nestedError = nestedTask.path("error");
                if (nestedError.isObject() && blank(task.getErrorMessage())) {
                    task.setErrorMessage(firstText(nestedError, "message", "code"));
                }
            }
            if (blank(task.getTaskId())) task.setTaskId(fallbackTaskId);
            if (blank(task.getTaskId())) {
                throw new IllegalStateException("视频模型未返回任务 ID");
            }
            if ("succeeded".equalsIgnoreCase(task.getStatus())) task.setStatus("SUCCESS");
            if ("failed".equalsIgnoreCase(task.getStatus())) task.setStatus("FAILED");
            if ("cancelled".equalsIgnoreCase(task.getStatus()) || "expired".equalsIgnoreCase(task.getStatus())) {
                task.setStatus("FAILED");
            }
            return task;
        } catch (Exception e) {
            if (e instanceof IllegalStateException) throw (IllegalStateException) e;
            throw new IllegalStateException("视频模型返回格式不正确", e);
        }
    }

    private String joinPrompt(VideoProviderRequest request) {
        if (blank(request.getStylePrompt())) return request.getPrompt();
        return request.getStylePrompt() + "\n" + request.getPrompt();
    }

    private boolean isSeed3dModel(AiModelConfigSnapshot config) {
        return config.getModelName() != null && config.getModelName().toLowerCase().contains("seed3d");
    }

    private boolean isMiniMax(AiModelConfigSnapshot config) {
        return "minimax".equalsIgnoreCase(config.getProviderType());
    }

    private String supportedResolution(String value, AiModelConfigSnapshot config) {
        String normalized = blank(value) ? "768P" : value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("720P", "768P", "1080P", "2K").contains(normalized)) {
            return "768P";
        }
        if ("MiniMax-H3".equalsIgnoreCase(config.getModelName()) && !Set.of("768P", "2K").contains(normalized)) {
            return "768P";
        }
        return normalized;
    }

    private String effectiveApiKey(AiModelConfigSnapshot config) {
        return config.getApiKey();
    }

    private String providerName(AiModelConfigSnapshot config) {
        return isMiniMax(config) ? "MiniMax " : "方舟";
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            String value = node.path(name).asText(null);
            if (!blank(value)) return value;
        }
        return null;
    }

    private void ensureConfigured(AiModelConfigSnapshot config) {
        if (!blank(config.getProviderType()) && !"ark".equalsIgnoreCase(config.getProviderType())
                && !"minimax".equalsIgnoreCase(config.getProviderType())) {
            throw new IllegalStateException("video.generation.provider 仅支持 ark 或 minimax");
        }
        if (!config.isEnabled() || blank(config.getApiUrl()) || blank(config.getQueryUrl()) || blank(config.getModelName())) {
            throw new IllegalStateException("请配置 video.generation.enabled、provider、endpoint、query-url 和 model");
        }
        if (blank(effectiveApiKey(config))) {
            String property = isMiniMax(config) ? "video.generation.minimax-api-key" : "video.generation.api-key";
            throw new IllegalStateException("请配置 " + property);
        }
    }

    private AiModelConfigSnapshot resolveVideoConfig() {
        if (modelConfigResolver != null) {
            return modelConfigResolver.resolveVideo();
        }
        return new AiModelConfigSnapshot("VIDEO", provider, endpoint, queryUrl,
                "minimax".equalsIgnoreCase(provider) ? minimaxApiKey : apiKey, model, enabled, false);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
