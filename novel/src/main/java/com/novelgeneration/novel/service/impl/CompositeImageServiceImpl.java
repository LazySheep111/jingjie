package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.mapper.CompositeImageTaskMapper;
import com.novelgeneration.novel.mapper.VisualAssetMapper;
import com.novelgeneration.novel.service.CompositeImageService;
import com.novelgeneration.novel.service.ModelConfigResolver;
import com.novelgeneration.novel.service.VisualStyleService;
import com.novelgeneration.novel.vo.AiModelConfigSnapshot;
import com.novelgeneration.novel.vo.CompositeImageTaskVO;
import com.novelgeneration.novel.vo.VisualAssetVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class CompositeImageServiceImpl implements CompositeImageService {

    @Resource
    private CompositeImageTaskMapper taskMapper;

    @Resource
    private VisualAssetMapper visualAssetMapper;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private VisualStyleService visualStyleService;

    @Resource
    private ModelConfigResolver modelConfigResolver;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${image.api-key:}")
    private String apiKey;

    @Value("${image.api-url:}")
    private String apiUrl;

    @Value("${image.api-model:}")
    private String apiModel;

    @Value("${image.local-dir:uploads/assets}")
    private String localDir;

    @Value("${image.reference-url:}")
    private String referenceImageUrl;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @Override
    public CompositeImageTaskVO start(Long novelId, Long assetId, VisualAssetVO request) {
        requireId(novelId, "novelId");
        requireId(assetId, "assetId");
        VisualAssetVO asset = visualAssetMapper.selectAssetVersion(assetId, null);
        if (asset == null || !novelId.equals(asset.getNovelId())) {
            throw new IllegalArgumentException("资产不存在");
        }
        applyPromptEdits(asset, request);
        if (blank(asset.getFrontPrompt()) || blank(asset.getSidePrompt()) || blank(asset.getBackPrompt())) {
            throw new IllegalArgumentException("请先完善三视图提示词");
        }
        CompositeImageTaskVO running = taskMapper.selectRunning(novelId, assetId, asset.getVersion());
        if (running != null) {
            if ("PENDING".equals(running.getStatus())) {
                executor.submit(() -> execute(running.getTaskId()));
            }
            return running;
        }
        CompositeImageTaskVO task = new CompositeImageTaskVO();
        task.setTaskId(UUID.randomUUID().toString());
        task.setNovelId(novelId);
        task.setAssetId(assetId);
        task.setAssetVersion(asset.getVersion());
        task.setStatus("PENDING");
        taskMapper.insert(task);
        executor.submit(() -> execute(task.getTaskId()));
        return task;
    }

    @Override
    public CompositeImageTaskVO getStatus(String taskId) {
        if (blank(taskId)) {
            throw new IllegalArgumentException("taskId 不能为空");
        }
        CompositeImageTaskVO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("图片生成任务不存在");
        }
        return task;
    }

    private void execute(String taskId) {
        if (taskMapper.claim(taskId) == 0) {
            return;
        }
        try {
            CompositeImageTaskVO task = getStatus(taskId);
            VisualAssetVO asset = visualAssetMapper.selectAssetVersion(task.getAssetId(), task.getAssetVersion());
            if (asset == null) {
                throw new IllegalArgumentException("资产版本不存在");
            }
            String imagePath = generateAndDownload(asset);
            visualAssetMapper.updateCompositeImagePath(asset.getAssetId(), asset.getVersion(), imagePath);
            taskMapper.complete(taskId, imagePath);
        } catch (Exception e) {
            log.error("Composite image generation failed: taskId={}", taskId, e);
            taskMapper.fail(taskId, safeMessage(e));
        }
    }

    private String generateAndDownload(VisualAssetVO asset) throws IOException {
        AiModelConfigSnapshot config = resolveImageConfig();
        if (config == null || !config.isEnabled() || blank(config.getApiKey()) || blank(config.getApiUrl())
                || blank(config.getModelName())) {
            throw new IllegalStateException("请配置 image.api-key、image.api-url 和 image.api-model");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());
        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getModelName());
        body.put("prompt", buildCompositePrompt(asset));
        if (!blank(referenceImageUrl)) {
            body.put("image", List.of(referenceImageUrl));
        }
        body.put("size", "2K");
        body.put("sequential_image_generation", "disabled");
        body.put("response_format", "b64_json");
        body.put("stream", false);
        body.put("watermark", false);
        ResponseEntity<String> response = restTemplate.postForEntity(resolveGenerationUrl(config.getApiUrl()), new HttpEntity<>(body, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful() || blank(response.getBody())) {
            throw new IllegalStateException("图片接口返回为空");
        }
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode imageData = root.path("data").path(0);
        byte[] imageBytes = null;
        String encodedImage = imageData.path("b64_json").asText(null);
        if (!blank(encodedImage)) {
            try {
                imageBytes = Base64.getDecoder().decode(encodedImage);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("图片接口返回的 b64_json 无效", e);
            }
        }
        if (imageBytes == null) {
            String remoteUrl = imageData.path("url").asText(null);
            if (blank(remoteUrl)) {
                throw new IllegalStateException("图片接口未返回 data[0].b64_json 或 data[0].url");
            }
            ResponseEntity<byte[]> imageResponse = restTemplate.getForEntity(remoteUrl, byte[].class);
            if (!imageResponse.getStatusCode().is2xxSuccessful() || imageResponse.getBody() == null) {
                throw new IllegalStateException("图片下载失败");
            }
            imageBytes = imageResponse.getBody();
        }
        Path directory = Paths.get(localDir).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String filename = "asset-" + asset.getAssetId() + "-v" + asset.getVersion() + "-" + UUID.randomUUID() + ".png";
        Files.write(directory.resolve(filename), imageBytes);
        return "/api/visual-assets/files/" + filename;
    }

    private String resolveGenerationUrl(String configuredUrl) {
        String normalized = configuredUrl.endsWith("/") ? configuredUrl.substring(0, configuredUrl.length() - 1) : configuredUrl;
        return normalized.endsWith("/images/generations")
                ? normalized
                : normalized + "/images/generations";
    }

    private AiModelConfigSnapshot resolveImageConfig() {
        if (modelConfigResolver != null) {
            return modelConfigResolver.resolveImage();
        }
        return new AiModelConfigSnapshot("IMAGE", "OPENAI_COMPATIBLE", apiUrl, null,
                apiKey, apiModel, true, false);
    }

    private String buildCompositePrompt(VisualAssetVO asset) {
        return "请生成一张角色或场景的三视图合成设定图。"
                + "同一张白色或浅色背景图片中，水平排列正面、侧面、背面三个视角，"
                + "保持主体外观、服装、比例、材质和颜色一致，并在图片底部用中文标注‘正面’、‘侧面’、‘背面’。"
                + "不要生成多张图片，不要改变资产设定。\n"
                + visualStyleService.buildPrompt(asset.getNovelId())
                + "正面提示词：" + asset.getFrontPrompt() + "\n"
                + "侧面提示词：" + asset.getSidePrompt() + "\n"
                + "背面提示词：" + asset.getBackPrompt();
    }

    private void applyPromptEdits(VisualAssetVO asset, VisualAssetVO request) {
        if (request == null) {
            return;
        }
        String frontPrompt = request.getFrontPrompt() == null ? asset.getFrontPrompt() : request.getFrontPrompt();
        String sidePrompt = request.getSidePrompt() == null ? asset.getSidePrompt() : request.getSidePrompt();
        String backPrompt = request.getBackPrompt() == null ? asset.getBackPrompt() : request.getBackPrompt();
        boolean changed = !safeEquals(asset.getFrontPrompt(), frontPrompt)
                || !safeEquals(asset.getSidePrompt(), sidePrompt)
                || !safeEquals(asset.getBackPrompt(), backPrompt);
        if (changed) {
            asset.setFrontPrompt(frontPrompt);
            asset.setSidePrompt(sidePrompt);
            asset.setBackPrompt(backPrompt);
            asset.setCompositeImagePath(null);
            asset.setStatus("PROMPT_READY");
            visualAssetMapper.updateAssetVersion(asset);
        }
    }

    private boolean safeEquals(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }

    private String safeMessage(Exception e) {
        return blank(e.getMessage()) ? "图片生成失败" : e.getMessage().substring(0, Math.min(e.getMessage().length(), 500));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void requireId(Long value, String name) {
        if (value == null || value < 1) {
            throw new IllegalArgumentException(name + " 必须大于 0");
        }
    }
}
