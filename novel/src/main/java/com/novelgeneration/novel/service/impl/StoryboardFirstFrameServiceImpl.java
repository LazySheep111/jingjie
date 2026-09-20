package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.StoryboardGenerationContext;
import com.novelgeneration.novel.mapper.StoryboardFirstFrameMapper;
import com.novelgeneration.novel.service.StoryboardFirstFrameService;
import com.novelgeneration.novel.service.StoryboardFrameGenerator;
import com.novelgeneration.novel.service.StoryboardGenerationPreparationService;
import com.novelgeneration.novel.vo.StoryboardFirstFrameVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class StoryboardFirstFrameServiceImpl implements StoryboardFirstFrameService {
    private static final long MAX_FIRST_FRAME_BYTES = 20L * 1024 * 1024;
    private static final Set<String> FIRST_FRAME_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
    private static final Set<String> FIRST_FRAME_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    @Resource
    private StoryboardFirstFrameMapper firstFrameMapper;

    @Resource
    private StoryboardGenerationPreparationService preparationService;

    @Resource
    private StoryboardFrameGenerator storyboardFrameGenerator;

    @Value("${image.storyboard-frame-dir:uploads/storyboard-frames}")
    private String storyboardFrameDir;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @Override
    public StoryboardFirstFrameVO createTask(Long novelId, Long chapterNum, Long sceneId) {
        requireId(novelId, "novelId");
        requireId(chapterNum, "chapterNum");
        requireId(sceneId, "sceneId");
        StoryboardFirstFrameVO running = firstFrameMapper.selectRunning(novelId, chapterNum, sceneId);
        if (running != null) {
            return running;
        }
        StoryboardGenerationContext context = preparationService.prepare(novelId, chapterNum, sceneId, null);
        StoryboardFirstFrameVO task = new StoryboardFirstFrameVO();
        task.setNovelId(novelId);
        task.setChapterNum(chapterNum);
        task.setSceneId(sceneId);
        Integer version = firstFrameMapper.nextVersion(sceneId);
        task.setVersion(version == null ? 1 : version);
        task.setStatus("QUEUED");
        task.setSource("AI");
        task.setPromptSnapshot(context.getFrameRequest().getPrompt());
        task.setStyleSnapshot(context.getStylePrompt());
        firstFrameMapper.insert(task);
        executor.submit(() -> generate(task.getId(), context));
        return task;
    }

    @Override
    public StoryboardFirstFrameVO upload(Long novelId, Long chapterNum, Long sceneId, MultipartFile file) {
        requireId(novelId, "novelId");
        requireId(chapterNum, "chapterNum");
        requireId(sceneId, "sceneId");
        String extension = validateUpload(file);
        byte[] content = readUpload(file);
        validateImageContent(content, extension);

        Path storedFile = null;
        try {
            storedFile = storeUpload(novelId, chapterNum, sceneId, extension, content);
            StoryboardFirstFrameVO frame = new StoryboardFirstFrameVO();
            frame.setNovelId(novelId);
            frame.setChapterNum(chapterNum);
            frame.setSceneId(sceneId);
            Integer version = firstFrameMapper.nextVersion(sceneId);
            frame.setVersion(version == null ? 1 : version);
            frame.setStatus("SUCCESS");
            frame.setSource("UPLOAD");
            frame.setImagePath(toWebPath(novelId, chapterNum, sceneId, storedFile));
            firstFrameMapper.insert(frame);
            return frame;
        } catch (IOException e) {
            deleteQuietly(storedFile);
            throw new IllegalStateException("保存首帧图片失败", e);
        } catch (RuntimeException e) {
            deleteQuietly(storedFile);
            throw e;
        }
    }

    @Override
    public StoryboardFirstFrameVO getTask(Long firstFrameId) {
        requireId(firstFrameId, "firstFrameId");
        StoryboardFirstFrameVO task = firstFrameMapper.selectById(firstFrameId);
        if (task == null) {
            throw new IllegalArgumentException("分镜首帧不存在");
        }
        return task;
    }

    @Override
    public List<StoryboardFirstFrameVO> listByScene(Long novelId, Long chapterNum, Long sceneId) {
        requireId(novelId, "novelId");
        requireId(chapterNum, "chapterNum");
        requireId(sceneId, "sceneId");
        return firstFrameMapper.selectByScene(novelId, chapterNum, sceneId);
    }

    @Override
    public void delete(Long firstFrameId) {
        StoryboardFirstFrameVO frame = getTask(firstFrameId);
        if (Boolean.TRUE.equals(frame.getDeleted())) {
            return;
        }
        firstFrameMapper.softDelete(firstFrameId);
    }

    private void generate(Long firstFrameId, StoryboardGenerationContext context) {
        try {
            firstFrameMapper.updateStatus(firstFrameId, "GENERATING", null, null);
            String imagePath = storyboardFrameGenerator.generate(context.getFrameRequest());
            firstFrameMapper.updateStatus(firstFrameId, "SUCCESS", imagePath, null);
        } catch (Exception e) {
            log.error("Storyboard first-frame generation failed: firstFrameId={}", firstFrameId, e);
            firstFrameMapper.updateStatus(firstFrameId, "FAILED", null, safeMessage(e));
        }
    }

    private String validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的首帧图片");
        }
        if (file.getSize() > MAX_FIRST_FRAME_BYTES) {
            throw new IllegalArgumentException("首帧图片不能超过 20 MB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!FIRST_FRAME_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("首帧仅支持 PNG、JPG、JPEG 或 WebP 图片");
        }
        String contentType = normalize(file.getContentType());
        if (!FIRST_FRAME_CONTENT_TYPES.contains(contentType)
                || ("png".equals(extension) && !"image/png".equals(contentType))
                || (("jpg".equals(extension) || "jpeg".equals(extension)) && !"image/jpeg".equals(contentType))
                || ("webp".equals(extension) && !"image/webp".equals(contentType))) {
            throw new IllegalArgumentException("首帧图片格式与文件类型不匹配");
        }
        return extension;
    }

    private byte[] readUpload(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("读取首帧图片失败", e);
        }
    }

    private void validateImageContent(byte[] content, String extension) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("首帧图片不能为空");
        }
        if ("webp".equals(extension)) {
            if (content.length < 12 || content[0] != 'R' || content[1] != 'I' || content[2] != 'F' || content[3] != 'F'
                    || content[8] != 'W' || content[9] != 'E' || content[10] != 'B' || content[11] != 'P') {
                throw new IllegalArgumentException("首帧图片内容不是有效的 WebP 文件");
            }
            return;
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(content)) {
            if (ImageIO.read(input) == null) {
                throw new IllegalArgumentException("首帧图片内容无法识别");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("首帧图片内容无法识别", e);
        }
    }

    private Path storeUpload(Long novelId, Long chapterNum, Long sceneId, String extension, byte[] content) throws IOException {
        Path root = Paths.get(storyboardFrameDir).toAbsolutePath().normalize();
        Path directory = root.resolve(String.valueOf(novelId)).resolve("chapter-" + chapterNum)
                .resolve("scene-" + sceneId).normalize();
        if (!directory.startsWith(root)) {
            throw new IllegalArgumentException("首帧图片保存路径不合法");
        }
        Files.createDirectories(directory);
        Path file = directory.resolve("frame-upload-" + UUID.randomUUID() + "." + extension).normalize();
        if (!file.startsWith(directory)) {
            throw new IllegalArgumentException("首帧图片保存路径不合法");
        }
        Files.write(file, content);
        return file;
    }

    private String toWebPath(Long novelId, Long chapterNum, Long sceneId, Path file) {
        return "/api/storyboard-frames/files/" + novelId + "/chapter-" + chapterNum
                + "/scene-" + sceneId + "/" + file.getFileName();
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int index = filename.lastIndexOf('.');
        return index < 1 || index == filename.length() - 1 ? "" : filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            log.warn("Unable to remove failed first-frame upload: {}", file);
        }
    }

    private void requireId(Long value, String name) {
        if (value == null || value < 1) {
            throw new IllegalArgumentException(name + " 必须大于 0");
        }
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? "分镜首帧生成失败" : message.substring(0, Math.min(500, message.length()));
    }
}
