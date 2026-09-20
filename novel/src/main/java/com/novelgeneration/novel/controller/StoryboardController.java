package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.dto.StoryboardGenerateRequest;
import com.novelgeneration.novel.service.StoryboardService;
import com.novelgeneration.novel.vo.StoryboardVO;
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

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/novel/{novelId}/chapters/{chapterNum}/storyboard")
public class StoryboardController {

    @Resource
    private StoryboardService storyboardService;

    @PostMapping({"/generate", "/regenerate"})
    public Result generate(@PathVariable Long novelId,
                           @PathVariable Long chapterNum,
                           @RequestBody StoryboardGenerateRequest request) {
        request.setChapterNum(chapterNum);
        request.setNovelId(novelId);
        return Result.ok(storyboardService.generate(novelId, request));
    }

    @GetMapping
    public Result get(@PathVariable Long novelId, @PathVariable Long chapterNum) {
        return Result.ok(storyboardService.getLatest(novelId, chapterNum));
    }

    @PutMapping
    public Result update(@PathVariable Long novelId,
                         @PathVariable Long chapterNum,
                         @RequestBody StoryboardVO storyboard) {
        return Result.ok(storyboardService.update(novelId, chapterNum, storyboard));
    }

    @DeleteMapping("/{sceneId}/assets/{assetId}")
    public Result removeAssetReference(@PathVariable Long novelId,
                                       @PathVariable Long chapterNum,
                                       @PathVariable Long sceneId,
                                       @PathVariable Long assetId,
                                       @RequestParam String assetRole) {
        storyboardService.removeAssetReference(novelId, chapterNum, sceneId, assetId, assetRole);
        return Result.ok();
    }

    @GetMapping(value = "/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> export(@PathVariable Long novelId, @PathVariable Long chapterNum) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=storyboard-" + chapterNum + ".txt")
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(storyboardService.exportText(novelId, chapterNum));
    }
}
