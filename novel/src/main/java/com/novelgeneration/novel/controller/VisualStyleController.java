package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.service.VisualStyleService;
import com.novelgeneration.novel.vo.VisualStyleVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/novel/{novelId}/visual-style")
public class VisualStyleController {
    @Resource
    private VisualStyleService visualStyleService;

    @GetMapping
    public Result get(@PathVariable Long novelId) {
        return Result.ok(visualStyleService.get(novelId));
    }

    @PutMapping
    public Result save(@PathVariable Long novelId, @RequestBody VisualStyleVO request) {
        return Result.ok(visualStyleService.save(novelId, request));
    }

    @PostMapping("/generate")
    public Result generate(@PathVariable Long novelId) {
        return Result.ok(visualStyleService.generateInitial(novelId));
    }
}
