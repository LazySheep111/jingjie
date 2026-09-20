package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.service.CompositeImageService;
import com.novelgeneration.novel.vo.VisualAssetVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api")
public class CompositeImageTaskController {

    @javax.annotation.Resource
    private CompositeImageService compositeImageService;

    @Value("${image.local-dir:uploads/assets}")
    private String localDir;

    @PostMapping("/novel/{novelId}/assets/{assetId}/composite-image")
    public Result start(@PathVariable Long novelId,
                        @PathVariable Long assetId,
                        @RequestBody(required = false) VisualAssetVO request) {
        return Result.ok(compositeImageService.start(novelId, assetId, request));
    }

    @GetMapping("/composite-image-tasks/{taskId}")
    public Result status(@PathVariable String taskId) {
        return Result.ok(compositeImageService.getStatus(taskId));
    }

    @GetMapping("/visual-assets/files/{filename:.+}")
    public ResponseEntity<Resource> file(@PathVariable String filename) {
        Path root = Paths.get(localDir).toAbsolutePath().normalize();
        Path file = root.resolve(filename).normalize();
        if (!file.startsWith(root) || !java.nio.file.Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(new FileSystemResource(file));
    }
}
