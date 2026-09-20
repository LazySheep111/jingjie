package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.service.HistoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RequestMapping("/api/novel")
@RestController
public class HistoryController {

    @Resource
    private HistoryService historyService;

    @GetMapping("/history")
    public Result history(@RequestParam(required = false, defaultValue = "1") Integer page,
                          @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.ok(historyService.listHistory(page, pageSize));
    }

    @GetMapping("/{novelId}/outline")
    public Result outline(@PathVariable Long novelId) {
        return Result.ok(historyService.getOutline(novelId));
    }

    @DeleteMapping("/{novelId}")
    public Result delete(@PathVariable Long novelId) {
        historyService.deleteNovel(novelId);
        return Result.ok();
    }
}
