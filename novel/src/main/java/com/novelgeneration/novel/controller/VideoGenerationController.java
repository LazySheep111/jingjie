package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.dto.VideoGenerationRequest;
import com.novelgeneration.novel.dto.VideoPromptRewriteRequest;
import com.novelgeneration.novel.service.StoryboardFirstFrameService;
import com.novelgeneration.novel.service.VideoGenerationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api")
public class VideoGenerationController {
    @javax.annotation.Resource
    private VideoGenerationService videoGenerationService;

    @javax.annotation.Resource
    private StoryboardFirstFrameService storyboardFirstFrameService;

    @Value("${video.generation.download-dir:uploads/videos}")
    private String downloadDir;

    @Value("${image.storyboard-frame-dir:uploads/storyboard-frames}")
    private String storyboardFrameDir;

    @PostMapping("/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/video-tasks")
    public Result create(@PathVariable Long novelId,
                         @PathVariable Long chapterNum,
                         @PathVariable Long sceneId,
                         @RequestBody(required = false) VideoGenerationRequest request) {
        Long requestedSceneId = request == null || request.getSceneId() == null ? sceneId : request.getSceneId();
        if (!sceneId.equals(requestedSceneId)) {
            return Result.fail("请求中的分镜 ID 不一致");
        }
        return Result.ok(videoGenerationService.createTask(novelId, chapterNum, sceneId,
                request == null ? null : request.getFirstFrameId(),
                request == null ? null : request.getPromptOverride(),
                request == null ? null : request.getResolution()));
    }

    @PostMapping("/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/first-frame-tasks")
    public Result createFirstFrame(@PathVariable Long novelId,
                                   @PathVariable Long chapterNum,
                                   @PathVariable Long sceneId) {
        return Result.ok(storyboardFirstFrameService.createTask(novelId, chapterNum, sceneId));
    }

    @PostMapping(value = "/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/first-frames/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadFirstFrame(@PathVariable Long novelId,
                                   @PathVariable Long chapterNum,
                                   @PathVariable Long sceneId,
                                   @RequestParam("file") MultipartFile file) {
        return Result.ok(storyboardFirstFrameService.upload(novelId, chapterNum, sceneId, file));
    }

    @PostMapping(value = "/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/videos/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadVideo(@PathVariable Long novelId,
                              @PathVariable Long chapterNum,
                              @PathVariable Long sceneId,
                              @RequestParam("file") MultipartFile file) {
        return Result.ok(videoGenerationService.upload(novelId, chapterNum, sceneId, file));
    }

    @GetMapping("/first-frame-tasks/{firstFrameId}")
    public Result firstFrameStatus(@PathVariable Long firstFrameId) {
        return Result.ok(storyboardFirstFrameService.getTask(firstFrameId));
    }

    @GetMapping("/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/first-frames")
    public Result firstFrames(@PathVariable Long novelId,
                              @PathVariable Long chapterNum,
                              @PathVariable Long sceneId) {
        return Result.ok(storyboardFirstFrameService.listByScene(novelId, chapterNum, sceneId));
    }

    @DeleteMapping("/storyboard-first-frames/{firstFrameId}")
    public Result deleteFirstFrame(@PathVariable Long firstFrameId) {
        storyboardFirstFrameService.delete(firstFrameId);
        return Result.ok();
    }

    @PostMapping("/video-prompts/safety-rewrite")
    public Result safetyRewrite(@RequestBody VideoPromptRewriteRequest request) {
        if (request == null) {
            return Result.fail("缺少待改写的视频提示词");
        }
        return Result.ok(videoGenerationService.rewritePromptForSafety(request.getPrompt()));
    }

    @GetMapping("/video-tasks/{taskId}")
    public Result status(@PathVariable Long taskId) {
        return Result.ok(videoGenerationService.getTask(taskId));
    }

    @GetMapping("/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/videos")
    public Result videos(@PathVariable Long novelId,
                         @PathVariable Long chapterNum,
                         @PathVariable Long sceneId) {
        return Result.ok(videoGenerationService.listVideos(novelId, chapterNum, sceneId));
    }

    @PostMapping("/video-tasks/{taskId}/retry")
    public Result retry(@PathVariable Long taskId,
                        @RequestBody(required = false) VideoGenerationRequest request) {
        return Result.ok(videoGenerationService.retry(taskId,
                request == null ? null : request.getPromptOverride()));
    }

    @PutMapping("/storyboard-videos/{videoId}/current")
    public Result setCurrent(@PathVariable Long videoId) {
        videoGenerationService.setCurrent(videoId);
        return Result.ok();
    }

    @DeleteMapping("/storyboard-videos/{videoId}")
    public Result deleteVideo(@PathVariable Long videoId) {
        return Result.ok(videoGenerationService.delete(videoId));
    }

    @GetMapping("/storyboard-videos/files/{novelId}/chapter-{chapterNum}/scene-{sceneId}/{filename:.+}")
    public ResponseEntity<Resource> file(@PathVariable Long novelId,
                                         @PathVariable Long chapterNum,
                                         @PathVariable Long sceneId,
                                         @PathVariable String filename) {
        Path root = Paths.get(downloadDir).toAbsolutePath().normalize();
        Path file = root.resolve(String.valueOf(novelId)).resolve("chapter-" + chapterNum)
                .resolve("scene-" + sceneId).resolve(filename).normalize();
        if (!file.startsWith(root) || !Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                .contentType(mediaType(file, MediaType.APPLICATION_OCTET_STREAM))
                .body(new FileSystemResource(file));
    }

    @GetMapping("/storyboard-frames/files/{novelId}/chapter-{chapterNum}/scene-{sceneId}/{filename:.+}")
    public ResponseEntity<Resource> frame(@PathVariable Long novelId,
                                          @PathVariable Long chapterNum,
                                          @PathVariable Long sceneId,
                                          @PathVariable String filename) {
        Path root = Paths.get(storyboardFrameDir).toAbsolutePath().normalize();
        Path file = root.resolve(String.valueOf(novelId)).resolve("chapter-" + chapterNum)
                .resolve("scene-" + sceneId).resolve(filename).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(new FileSystemResource(file));
    }

    private MediaType mediaType(Path file, MediaType fallback) {
        try {
            String detected = Files.probeContentType(file);
            return detected == null || detected.isBlank() ? fallback : MediaType.parseMediaType(detected);
        } catch (IOException | IllegalArgumentException e) {
            return fallback;
        }
    }
}
