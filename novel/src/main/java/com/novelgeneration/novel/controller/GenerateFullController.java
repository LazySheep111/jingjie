package com.novelgeneration.novel.controller;


import com.novelgeneration.novel.dto.OutlineDTO;
import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.service.GenerateFullService;
import com.novelgeneration.novel.service.GenerateOutlineService;
import com.novelgeneration.novel.service.GenerateStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RequestMapping({"/api/novel"})
@RestController
public class GenerateFullController {

    @Resource
    private GenerateFullService generateFullService;

    @Resource
    private GenerateStatusService generateStatusService;

    @PostMapping("/{novelId}/generateFull")
    public Result generateFull(@PathVariable Long novelId,@RequestBody OutlineDTO outlineDTO) {
        return generateFullService.GenerateFull(novelId,outlineDTO);
    }

    @GetMapping("/{novelId}/generateStatus")
    public Result generateStatus(@PathVariable Long novelId) {
        return generateStatusService.generatestaus(novelId);
    }
}
