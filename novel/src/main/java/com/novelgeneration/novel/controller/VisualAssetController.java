package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.AssetMergeRequest;
import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.service.VisualAssetService;
import com.novelgeneration.novel.vo.VisualAssetVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/novel/{novelId}")
public class VisualAssetController {

    @Resource
    private VisualAssetService visualAssetService;

    @GetMapping("/assets")
    public Result list(@PathVariable Long novelId,
                       @RequestParam(required = false) String assetType) {
        return Result.ok(visualAssetService.list(novelId, assetType));
    }

    @GetMapping("/assets/{assetId}")
    public Result get(@PathVariable Long novelId, @PathVariable Long assetId) {
        return Result.ok(visualAssetService.get(novelId, assetId));
    }

    @PutMapping("/assets/{assetId}")
    public Result update(@PathVariable Long novelId,
                         @PathVariable Long assetId,
                         @RequestBody VisualAssetVO request) {
        return Result.ok(visualAssetService.update(novelId, assetId, request));
    }

    @PostMapping("/assets/{assetId}/merge")
    public Result merge(@PathVariable Long novelId,
                        @PathVariable Long assetId,
                        @RequestBody AssetMergeRequest request) {
        return Result.ok(visualAssetService.merge(novelId, assetId, request));
    }

    @GetMapping("/chapters/{chapterNum}/assets")
    public Result listByChapter(@PathVariable Long novelId, @PathVariable Long chapterNum) {
        return Result.ok(visualAssetService.listByChapter(novelId, chapterNum));
    }
}
