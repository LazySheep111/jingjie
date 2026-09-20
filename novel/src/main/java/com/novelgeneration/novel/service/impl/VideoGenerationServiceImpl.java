package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.StoryboardGenerationContext;
import com.novelgeneration.novel.dto.VideoProviderRequest;
import com.novelgeneration.novel.mapper.StoryboardFirstFrameMapper;
import com.novelgeneration.novel.mapper.StoryboardVideoMapper;
import com.novelgeneration.novel.mapper.VideoGenerationTaskMapper;
import com.novelgeneration.novel.service.StoryboardGenerationPreparationService;
import com.novelgeneration.novel.service.VideoGenerationClient;
import com.novelgeneration.novel.service.VideoGenerationService;
import com.novelgeneration.novel.vo.StoryboardFirstFrameVO;
import com.novelgeneration.novel.vo.StoryboardVO;
import com.novelgeneration.novel.vo.StoryboardVideoVO;
import com.novelgeneration.novel.vo.VideoGenerationTaskVO;
import com.novelgeneration.novel.utils.AiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class VideoGenerationServiceImpl implements VideoGenerationService {
    private static final long MAX_VIDEO_BYTES = 500L * 1024 * 1024;
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");
    private static final Set<String> VIDEO_CONTENT_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");

    @Resource
    private VideoGenerationTaskMapper taskMapper;

    @Resource
    private StoryboardFirstFrameMapper firstFrameMapper;

    @Resource
    private StoryboardVideoMapper videoMapper;

    @Resource
    private StoryboardGenerationPreparationService preparationService;

    @Resource
    private VideoGenerationClient videoClient;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private AiUtil aiUtil;

    @Value("${video.generation.download-dir:uploads/videos}")
    private String downloadDir;

    @Value("${video.generation.poll-interval-ms:2000}")
    private long pollIntervalMs;

    @Value("${video.generation.timeout-ms:1800000}")
    private long timeoutMs;

    private ExecutorService executor = Executors.newFixedThreadPool(2);

    @Override
    public VideoGenerationTaskVO createTask(Long novelId, Long chapterNum, Long sceneId, Long firstFrameId) {
        return createTask(novelId, chapterNum, sceneId, firstFrameId, null);
    }

    @Override
    public VideoGenerationTaskVO createTask(Long novelId, Long chapterNum, Long sceneId,
                                             Long firstFrameId, String promptOverride) {
        return createTask(novelId, chapterNum, sceneId, firstFrameId, promptOverride, null, null);
    }

    @Override
    public VideoGenerationTaskVO createTask(Long novelId, Long chapterNum, Long sceneId,
                                             Long firstFrameId, String promptOverride, String resolution) {
        return createTask(novelId, chapterNum, sceneId, firstFrameId, promptOverride, resolution, null);
    }

    private VideoGenerationTaskVO createTask(Long novelId, Long chapterNum, Long sceneId,
                                               Long firstFrameId, String promptOverride, String resolution,
                                               String reusableFirstFramePath) {
        requireId(novelId, "novelId");
        requireId(chapterNum, "chapterNum");
        requireId(sceneId, "sceneId");
        if (blank(reusableFirstFramePath) && firstFrameId == null) {
            throw new IllegalArgumentException("请先选择当前分镜的有效首帧");
        }
        VideoGenerationTaskVO running = taskMapper.selectRunning(novelId, chapterNum, sceneId);
        if (running != null) {
            return enrichTask(running);
        }
        String firstFramePath = reusableFirstFramePath;
        if (blank(firstFramePath)) {
            firstFramePath = selectedFirstFramePath(novelId, chapterNum, sceneId, firstFrameId);
        }
        StoryboardGenerationContext context = preparationService.prepare(novelId, chapterNum, sceneId, promptOverride);
        PreparedRequest prepared = new PreparedRequest();
        prepared.novelId = novelId;
        prepared.chapterNum = chapterNum;
        prepared.request = context.getVideoRequest();
        prepared.request.setResolution(normalizeResolution(resolution));
        prepared.request.setFirstFrameImageUrl(preparationService.toProviderImagePath(firstFramePath));
        prepared.assets = context.getAssetSnapshots();
        prepared.scene = context.getScene();
        prepared.firstFramePath = firstFramePath;
        VideoGenerationTaskVO task = new VideoGenerationTaskVO();
        task.setNovelId(novelId);
        task.setChapterNum(chapterNum);
        task.setSceneId(sceneId);
        task.setFirstFrameId(firstFrameId);
        task.setFirstFramePath(firstFramePath);
        task.setResolution(prepared.request.getResolution());
        task.setStatus("QUEUED");
        taskMapper.insert(task);
        executor.submit(() -> execute(task.getTaskId(), prepared));
        return enrichTask(task);
    }

    @Override
    public String rewritePromptForSafety(String prompt) {
        if (blank(prompt)) {
            throw new IllegalArgumentException("待改写的视频提示词不能为空");
        }
        String instruction = "请将下面的视频生成提示词改写为适合公开影视生成模型的安全版本。"
                + "保留人物、场景、镜头、动作、情绪和叙事目的；将血腥、伤口、肢解、虐待、色情、未成年人危险行为等明确或刺激性描写改为克制、中性、非露骨的影视表达。"
                + "不要添加新的危险行为，不要改变故事核心，不要解释修改过程，只返回改写后的中文提示词。\n\n原提示词：\n" + prompt;
        return aiUtil.chat(instruction).trim();
    }

    @Override
    public VideoGenerationTaskVO getTask(Long taskId) {
        requireId(taskId, "taskId");
        VideoGenerationTaskVO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("视频生成任务不存在");
        }
        return enrichTask(task);
    }

    @Override
    public List<StoryboardVideoVO> listVideos(Long novelId, Long chapterNum, Long sceneId) {
        List<StoryboardVideoVO> videos = videoMapper.selectByScene(novelId, chapterNum, sceneId);
        for (StoryboardVideoVO video : videos) {
            video.setAssets(videoMapper.selectAssets(video.getId()));
        }
        return videos;
    }

    @Override
    @Transactional
    public StoryboardVideoVO upload(Long novelId, Long chapterNum, Long sceneId, MultipartFile file) {
        requireId(novelId, "novelId");
        requireId(chapterNum, "chapterNum");
        requireId(sceneId, "sceneId");
        String extension = validateVideoUpload(file);
        byte[] content = readUpload(file);
        validateVideoContent(content, extension);

        Path storedFile = null;
        try {
            storedFile = storeUploadedVideo(novelId, chapterNum, sceneId, extension, content);
            StoryboardVideoVO video = new StoryboardVideoVO();
            video.setNovelId(novelId);
            video.setChapterNum(chapterNum);
            video.setSceneId(sceneId);
            Integer version = videoMapper.nextVersion(sceneId);
            video.setVersion(version == null ? 1 : version);
            video.setSource("UPLOAD");
            video.setVideoPath(toWebPath(novelId, chapterNum, sceneId, storedFile));
            video.setCurrent(true);
            video.setDeleted(false);
            videoMapper.clearCurrent(sceneId);
            videoMapper.insert(video);
            return video;
        } catch (IOException e) {
            deleteQuietly(storedFile);
            throw new IllegalStateException("保存上传视频失败", e);
        } catch (RuntimeException e) {
            deleteQuietly(storedFile);
            throw e;
        }
    }

    @Override
    @Transactional
    public StoryboardVideoVO delete(Long videoId) {
        requireId(videoId, "videoId");
        StoryboardVideoVO video = videoMapper.selectById(videoId);
        if (video == null || Boolean.TRUE.equals(video.getDeleted())) {
            throw new IllegalArgumentException("视频版本不存在");
        }
        videoMapper.softDelete(videoId);
        videoMapper.clearCurrent(video.getSceneId());
        StoryboardVideoVO fallback = videoMapper.selectLatestAvailableByScene(video.getSceneId());
        if (fallback != null) {
            videoMapper.setCurrent(fallback.getId());
            fallback.setCurrent(true);
        }
        return fallback;
    }

    @Override
    public VideoGenerationTaskVO retry(Long taskId) {
        return retry(taskId, null);
    }

    @Override
    public VideoGenerationTaskVO retry(Long taskId, String promptOverride) {
        VideoGenerationTaskVO old = getTask(taskId);
        if (!"FAILED".equals(old.getStatus())) {
            throw new IllegalArgumentException("只有失败任务可以重试");
        }
        return createTask(old.getNovelId(), old.getChapterNum(), old.getSceneId(), old.getFirstFrameId(), promptOverride,
                old.getResolution(),
                old.getFirstFramePath());
    }

    @Override
    public void setCurrent(Long videoId) {
        requireId(videoId, "videoId");
        Long sceneId = videoMapper.selectSceneId(videoId);
        if (sceneId == null) {
            throw new IllegalArgumentException("视频版本不存在");
        }
        videoMapper.clearCurrent(sceneId);
        videoMapper.setCurrent(videoId);
    }

    private void execute(Long taskId, PreparedRequest prepared) {
        try {
            taskMapper.updateStatus(taskId, "GENERATING_VIDEO", null, null);
            VideoGenerationClient.ProviderTask provider = videoClient.submit(prepared.request);
            taskMapper.updateStatus(taskId, "GENERATING_VIDEO", provider.getTaskId(), null);
            provider = waitForResult(provider);
            if (blank(provider.getVideoUrl())) {
                throw new IllegalStateException(blank(provider.getErrorMessage()) ? "视频模型未返回视频地址" : provider.getErrorMessage());
            }
            Path localPath = download(provider.getVideoUrl(), prepared);
            StoryboardVideoVO video = new StoryboardVideoVO();
            video.setNovelId(prepared.novelId);
            video.setChapterNum(prepared.chapterNum);
            video.setSceneId(prepared.scene.getId());
            video.setVersion(videoMapper.nextVersion(prepared.scene.getId()));
            video.setSource("AI");
            video.setVideoPath(toWebPath(localPath, prepared));
            video.setFirstFramePath(prepared.firstFramePath);
            video.setDurationSec(prepared.request.getDurationSec());
            video.setResolution(prepared.request.getResolution());
            video.setPromptSnapshot(prepared.request.getPrompt());
            video.setStyleSnapshot(prepared.request.getStylePrompt());
            videoMapper.clearCurrent(prepared.scene.getId());
            videoMapper.insert(video);
            for (StoryboardVideoVO.AssetSnapshot asset : prepared.assets) {
                asset.setVideoId(video.getId());
                videoMapper.insertAsset(asset);
            }
            taskMapper.updateStatus(taskId, "SUCCESS", provider.getTaskId(), null);
        } catch (Exception e) {
            log.error("Video generation failed: taskId={}", taskId, e);
            taskMapper.updateStatus(taskId, "FAILED", null, safeMessage(e));
        }
    }

    private VideoGenerationClient.ProviderTask waitForResult(VideoGenerationClient.ProviderTask task) throws InterruptedException {
        if (!blank(task.getVideoUrl()) || "SUCCESS".equalsIgnoreCase(task.getStatus())) {
            return task;
        }
        long deadline = System.currentTimeMillis() + timeoutMs;
        VideoGenerationClient.ProviderTask current = task;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(Math.max(250, pollIntervalMs));
            current = videoClient.query(task.getTaskId());
            if (!blank(current.getVideoUrl()) || "SUCCESS".equalsIgnoreCase(current.getStatus())) {
                return current;
            }
            if ("FAILED".equalsIgnoreCase(current.getStatus()) || "ERROR".equalsIgnoreCase(current.getStatus())) {
                return current;
            }
        }
        throw new IllegalStateException("视频生成任务超时");
    }

    private Path download(String url, PreparedRequest prepared) throws IOException {
        byte[] bytes = downloadBytes(url);
        if (bytes == null || bytes.length == 0) {
            throw new IOException("视频下载为空");
        }
        Path directory = Paths.get(downloadDir, String.valueOf(prepared.novelId), "chapter-" + prepared.chapterNum,
                "scene-" + prepared.scene.getId()).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path file = directory.resolve("video-" + UUID.randomUUID() + ".mp4");
        Files.write(file, bytes);
        return file;
    }

    private String validateVideoUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的视频");
        }
        if (file.getSize() > MAX_VIDEO_BYTES) {
            throw new IllegalArgumentException("视频不能超过 500 MB");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!VIDEO_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("视频仅支持 MP4、WebM 或 MOV 格式");
        }
        String contentType = normalize(file.getContentType());
        if (!VIDEO_CONTENT_TYPES.contains(contentType)
                || ("mp4".equals(extension) && !"video/mp4".equals(contentType))
                || ("webm".equals(extension) && !"video/webm".equals(contentType))
                || ("mov".equals(extension) && !"video/quicktime".equals(contentType))) {
            throw new IllegalArgumentException("视频格式与文件类型不匹配");
        }
        return extension;
    }

    private byte[] readUpload(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("读取上传视频失败", e);
        }
    }

    private void validateVideoContent(byte[] content, String extension) {
        if (content == null || content.length < 4) {
            throw new IllegalArgumentException("视频内容无法识别");
        }
        if ("webm".equals(extension)) {
            if ((content[0] & 0xFF) != 0x1A || (content[1] & 0xFF) != 0x45
                    || (content[2] & 0xFF) != 0xDF || (content[3] & 0xFF) != 0xA3) {
                throw new IllegalArgumentException("视频内容不是有效的 WebM 文件");
            }
            return;
        }
        int limit = Math.min(content.length - 3, 4096);
        for (int index = 4; index < limit; index++) {
            if (content[index] == 'f' && content[index + 1] == 't'
                    && content[index + 2] == 'y' && content[index + 3] == 'p') {
                return;
            }
        }
        throw new IllegalArgumentException("视频内容不是有效的 MP4 或 MOV 文件");
    }

    private Path storeUploadedVideo(Long novelId, Long chapterNum, Long sceneId, String extension, byte[] content) throws IOException {
        Path root = Paths.get(downloadDir).toAbsolutePath().normalize();
        Path directory = root.resolve(String.valueOf(novelId)).resolve("chapter-" + chapterNum)
                .resolve("scene-" + sceneId).normalize();
        if (!directory.startsWith(root)) {
            throw new IllegalArgumentException("视频保存路径不合法");
        }
        Files.createDirectories(directory);
        Path file = directory.resolve("video-upload-" + UUID.randomUUID() + "." + extension).normalize();
        if (!file.startsWith(directory)) {
            throw new IllegalArgumentException("视频保存路径不合法");
        }
        Files.write(file, content);
        return file;
    }

    private byte[] downloadBytes(String url) {
        return restTemplate.getForObject(URI.create(url), byte[].class);
    }

    private String toWebPath(Path path, PreparedRequest prepared) {
        return "/api/storyboard-videos/files/" + prepared.novelId + "/chapter-" + prepared.chapterNum
                + "/scene-" + prepared.scene.getId() + "/" + path.getFileName();
    }

    private String toWebPath(Long novelId, Long chapterNum, Long sceneId, Path path) {
        return "/api/storyboard-videos/files/" + novelId + "/chapter-" + chapterNum
                + "/scene-" + sceneId + "/" + path.getFileName();
    }

    private String selectedFirstFramePath(Long novelId, Long chapterNum, Long sceneId, Long firstFrameId) {
        if (firstFrameId == null) {
            throw new IllegalArgumentException("请先选择当前分镜的有效首帧");
        }
        StoryboardFirstFrameVO frame = firstFrameMapper.selectAvailableById(firstFrameId);
        if (frame == null || !novelId.equals(frame.getNovelId()) || !chapterNum.equals(frame.getChapterNum())
                || !sceneId.equals(frame.getSceneId()) || blank(frame.getImagePath())) {
            throw new IllegalArgumentException("请先选择当前分镜的有效首帧");
        }
        return frame.getImagePath();
    }

    private VideoGenerationTaskVO enrichTask(VideoGenerationTaskVO task) {
        if ("SUCCESS".equals(task.getStatus())) {
            List<StoryboardVideoVO> videos = listVideos(task.getNovelId(), task.getChapterNum(), task.getSceneId());
            if (!videos.isEmpty()) {
                task.setVideoId(videos.get(0).getId());
                task.setVideoUrl(videos.get(0).getVideoPath());
            }
        }
        return task;
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return blank(message) ? "视频生成失败" : message.substring(0, Math.min(500, message.length()));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeResolution(String value) {
        if (blank(value)) {
            return "768P";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Set.of("720P", "768P", "1080P", "2K").contains(normalized) ? normalized : "768P";
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
            log.warn("Unable to remove failed uploaded video: {}", file);
        }
    }

    private void requireId(Long value, String name) {
        if (value == null || value < 1) {
            throw new IllegalArgumentException(name + " 必须大于 0");
        }
    }

    private static class PreparedRequest {
        private Long novelId;
        private Long chapterNum;
        private VideoProviderRequest request;
        private List<StoryboardVideoVO.AssetSnapshot> assets;
        private StoryboardVO.Scene scene;
        private String firstFramePath;
    }
}
