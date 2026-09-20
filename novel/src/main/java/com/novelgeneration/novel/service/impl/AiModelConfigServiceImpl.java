package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.AiModelConfigRequest;
import com.novelgeneration.novel.entity.AiModelConfig;
import com.novelgeneration.novel.mapper.AiModelConfigMapper;
import com.novelgeneration.novel.service.AiModelConfigService;
import com.novelgeneration.novel.utils.ApiKeyEncryptor;
import com.novelgeneration.novel.vo.AiModelConfigTestResult;
import com.novelgeneration.novel.vo.AiModelConfigVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class AiModelConfigServiceImpl implements AiModelConfigService {
    private static final List<String> CAPABILITIES = List.of("TEXT", "IMAGE", "VIDEO");

    @Resource
    private AiModelConfigMapper mapper;

    @Resource
    private ApiKeyEncryptor encryptor;

    @Resource(name = "restTemplate")
    private RestTemplate restTemplate;

    @Override
    public List<AiModelConfigVO> list() {
        return CAPABILITIES.stream().map(type -> toVO(type, mapper.selectActive(type))).toList();
    }

    @Override
    public AiModelConfigTestResult test(String capabilityType, AiModelConfigRequest request) {
        String type = normalizeCapability(capabilityType);
        validateRequest(type, request);
        String apiKey = effectiveApiKey(type, request);
        if (blank(apiKey)) {
            return AiModelConfigTestResult.failure("未填写 API Key，无法测试");
        }
        try {
            if ("TEXT".equals(type)) {
                testText(request, apiKey);
            } else {
                testReachability(request, apiKey);
            }
            return AiModelConfigTestResult.success("连接测试通过");
        } catch (HttpStatusCodeException e) {
            String detail = blank(e.getResponseBodyAsString()) ? e.getStatusText() : e.getResponseBodyAsString();
            return AiModelConfigTestResult.failure("连接测试失败（" + e.getStatusCode().value() + "）：" + truncate(detail));
        } catch (RestClientException e) {
            return AiModelConfigTestResult.failure("连接测试失败：" + truncate(e.getMessage()));
        } catch (RuntimeException e) {
            return AiModelConfigTestResult.failure("连接测试失败：" + truncate(e.getMessage()));
        }
    }

    @Override
    @Transactional
    public AiModelConfigVO save(String capabilityType, AiModelConfigRequest request) {
        String type = normalizeCapability(capabilityType);
        validateRequest(type, request);
        AiModelConfigTestResult testResult = test(type, request);
        if (!testResult.isSuccess()) {
            throw new IllegalStateException(testResult.getMessage());
        }

        AiModelConfig active = mapper.selectActive(type);
        String apiKey = effectiveApiKey(type, request);
        if (blank(apiKey)) {
            throw new IllegalArgumentException("API Key 不能为空");
        }
        AiModelConfig config = new AiModelConfig();
        config.setCapabilityType(type);
        config.setProviderType(normalizeProvider(type, request.getProviderType()));
        config.setApiUrl(request.getApiUrl().trim());
        config.setQueryUrl(blank(request.getQueryUrl()) ? null : request.getQueryUrl().trim());
        config.setEncryptedApiKey(encryptor.encrypt(apiKey));
        config.setModelName(request.getModelName().trim());
        config.setEnabled(true);
        config.setTestStatus("PASSED");
        config.setLastTestAt(LocalDateTime.now());
        config.setLastError(null);
        Integer latestVersion = mapper.selectLatestVersion(type);
        config.setVersion((latestVersion == null ? 0 : latestVersion) + 1);
        mapper.disableActive(type);
        mapper.insertVersion(config);
        return toVO(type, config);
    }

    @Override
    @Transactional
    public AiModelConfigVO disable(String capabilityType) {
        String type = normalizeCapability(capabilityType);
        mapper.disableActive(type);
        return toVO(type, mapper.selectActive(type));
    }

    private void testText(AiModelConfigRequest request, String apiKey) {
        HttpHeaders headers = headers(apiKey);
        Map<String, Object> message = Map.of("role", "user", "content", "连接测试");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.getModelName().trim());
        body.put("messages", List.of(message));
        body.put("max_tokens", 1);
        ResponseEntity<String> response = restTemplate.postForEntity(request.getApiUrl().trim(),
                new HttpEntity<>(body, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("服务返回状态异常");
        }
    }

    private void testReachability(AiModelConfigRequest request, String apiKey) {
        try {
            Map<String, Object> probe = new LinkedHashMap<>();
            probe.put("model", request.getModelName().trim());
            ResponseEntity<String> response = restTemplate.postForEntity(request.getApiUrl().trim(),
                    new HttpEntity<>(probe, headers(apiKey)), String.class);
            if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
                throw new IllegalStateException("服务返回状态异常");
            }
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            if (status == 400 || status == 422) {
                log.debug("API endpoint rejected the intentionally incomplete probe; treating it as reachable");
                return;
            }
            throw e;
        }
    }

    private HttpHeaders headers(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private AiModelConfigVO toVO(String type, AiModelConfig config) {
        AiModelConfigVO vo = new AiModelConfigVO();
        vo.setCapabilityType(type);
        if (config == null) {
            vo.setProviderType(defaultProvider(type));
            vo.setEnabled(false);
            vo.setTestStatus("NOT_CONFIGURED");
            vo.setApiKeyMasked("未配置");
            return vo;
        }
        vo.setId(config.getId());
        vo.setProviderType(config.getProviderType());
        vo.setApiUrl(config.getApiUrl());
        vo.setQueryUrl(config.getQueryUrl());
        vo.setModelName(config.getModelName());
        vo.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
        vo.setTestStatus(config.getTestStatus());
        vo.setLastTestAt(config.getLastTestAt());
        vo.setLastError(config.getLastError());
        vo.setVersion(config.getVersion());
        try {
            vo.setApiKeyMasked(encryptor.mask(encryptor.decrypt(config.getEncryptedApiKey())));
        } catch (RuntimeException e) {
            vo.setApiKeyMasked("已配置（密钥不可解密）");
            vo.setTestStatus("ERROR");
            vo.setLastError("加密密钥不可用，请检查 AI_CONFIG_ENCRYPTION_KEY");
        }
        return vo;
    }

    private String effectiveApiKey(String type, AiModelConfigRequest request) {
        if (!blank(request.getApiKey())) {
            return request.getApiKey().trim();
        }
        AiModelConfig active = mapper.selectActive(type);
        if (active == null || blank(active.getEncryptedApiKey())) {
            return null;
        }
        return encryptor.decrypt(active.getEncryptedApiKey());
    }

    private void validateRequest(String type, AiModelConfigRequest request) {
        if (request == null) throw new IllegalArgumentException("配置内容不能为空");
        normalizeProvider(type, request.getProviderType());
        validateUrl(request.getApiUrl(), "API 地址");
        if ("VIDEO".equals(type) && blank(request.getQueryUrl())) {
            throw new IllegalArgumentException("视频模型必须填写任务查询地址");
        }
        if (!blank(request.getQueryUrl())) {
            validateUrl(request.getQueryUrl(), "查询地址", true);
            if ("VIDEO".equals(type) && !request.getQueryUrl().contains("{taskId}")) {
                throw new IllegalArgumentException("查询地址必须包含 {taskId}");
            }
        }
        if (blank(request.getModelName())) throw new IllegalArgumentException("模型名称不能为空");
    }

    private String normalizeCapability(String value) {
        String type = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!CAPABILITIES.contains(type)) throw new IllegalArgumentException("不支持的模型能力类型");
        return type;
    }

    private String normalizeProvider(String type, String value) {
        String provider = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (("TEXT".equals(type) || "IMAGE".equals(type)) && !"OPENAI_COMPATIBLE".equals(provider)) {
            throw new IllegalArgumentException(type + " 仅支持 OPENAI_COMPATIBLE 服务商");
        }
        if ("VIDEO".equals(type) && !List.of("ARK", "MINIMAX").contains(provider)) {
            throw new IllegalArgumentException("VIDEO 仅支持 ARK 或 MINIMAX 服务商");
        }
        return provider;
    }

    private String defaultProvider(String type) {
        return "VIDEO".equals(type) ? "ARK" : "OPENAI_COMPATIBLE";
    }

    private void validateUrl(String value, String label) {
        validateUrl(value, label, false);
    }

    private void validateUrl(String value, String label, boolean allowTaskIdPlaceholder) {
        if (blank(value)) throw new IllegalArgumentException(label + "不能为空");
        try {
            String normalized = value.trim();
            if (allowTaskIdPlaceholder) {
                normalized = normalized.replace("{taskId}", "task-id");
            }
            URI uri = URI.create(normalized);
            if (!List.of("http", "https").contains(uri.getScheme())) throw new IllegalArgumentException();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(label + "必须是 http/https 地址");
        }
    }

    private String truncate(String value) {
        if (blank(value)) return "服务未返回错误详情";
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.substring(0, Math.min(normalized.length(), 240));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
