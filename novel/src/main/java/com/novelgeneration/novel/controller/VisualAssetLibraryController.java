package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.AssetReuseRequest;
import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.service.VisualAssetService;
import com.novelgeneration.novel.vo.VisualAssetVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api")
public class VisualAssetLibraryController {

    @Resource
    private VisualAssetService visualAssetService;

    @GetMapping("/novels")
    public Result novels() {
        return Result.ok(visualAssetService.listNovelsForLibrary());
    }

    @GetMapping("/visual-assets/library")
    public Result library(@RequestParam(required = false) Long novelId,
                          @RequestParam(required = false) String assetType,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false, defaultValue = "1") Integer page,
                          @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        return Result.ok(visualAssetService.listLibrary(novelId, assetType, keyword, page, pageSize));
    }

    @GetMapping("/visual-assets/{assetId}")
    public Result get(@PathVariable Long assetId) {
        return Result.ok(visualAssetService.getGlobal(assetId));
    }

    @PostMapping("/visual-assets/{assetId}/versions")
    public Result createVersion(@PathVariable Long assetId,
                                @RequestBody VisualAssetVO request) {
        if (request == null) {
            throw new IllegalArgumentException("资产内容不能为空");
        }
        return Result.ok(visualAssetService.createNextVersionGlobal(
                assetId,
                request.getCoreFeatures(),
                request.getFrontPrompt(),
                request.getSidePrompt(),
                request.getBackPrompt()));
    }

    @PostMapping("/visual-assets/{assetId}/reuse")
    public Result reuse(@PathVariable Long assetId,
                        @RequestBody AssetReuseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("复用参数不能为空");
        }
        visualAssetService.reuse(assetId, request.getTargetNovelId(),
                request.getTargetChapterNum(), request.getAssetRole(), request.getTargetAssetId());
        return Result.ok();
    }
}
