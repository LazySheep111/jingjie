package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.OutlineDTO;
import com.novelgeneration.novel.service.GenerateOutlineService;
import com.novelgeneration.novel.vo.NovelOutlineVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RequestMapping({"/api/novel", "/apo/novel"})
@RestController
public class OutlineController {

    @Resource
    private GenerateOutlineService generateOutlineService;

    @PostMapping("/generateOutline")
    public NovelOutlineVO generateOutline(@RequestBody OutlineDTO outlineDTO) {
        log.info("Receive generate outline request: {}", outlineDTO);
        return generateOutlineService.generateOutline(outlineDTO);
    }
}
