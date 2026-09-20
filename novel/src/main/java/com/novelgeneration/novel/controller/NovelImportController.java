package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.NovelImportResult;
import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.service.NovelImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/novel")
public class NovelImportController {
    @Resource
    private NovelImportService novelImportService;

    @PostMapping("/import")
    public Result importNovel(@RequestParam("file") MultipartFile file,
                              @RequestParam(value = "novelTitle", required = false) String novelTitle) {
        return Result.ok(novelImportService.importNovel(file, novelTitle));
    }
}
