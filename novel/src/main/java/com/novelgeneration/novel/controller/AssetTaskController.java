package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.AssetExtractRequest;
import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.service.AssetExtractionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api")
public class AssetTaskController {

    @Resource
    private AssetExtractionService assetExtractionService;

    @PostMapping("/novel/{novelId}/chapters/{chapterNum}/assets/extract")
    public Result start(@PathVariable Long novelId,
                        @PathVariable Long chapterNum,
                        @RequestBody(required = false) AssetExtractRequest request) {
        return Result.ok(assetExtractionService.start(novelId, chapterNum,
                request == null ? new AssetExtractRequest() : request));
    }

    @GetMapping("/asset-tasks/{taskId}")
    public Result status(@PathVariable String taskId) {
        return Result.ok(assetExtractionService.getStatus(taskId));
    }
}
