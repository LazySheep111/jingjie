package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.OutlineDTO;
import com.novelgeneration.novel.service.SaveOutlineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;

@Slf4j
@RequestMapping({"/api/novel"})
@RestController
public class SaveOutlineController {

    @Resource
    private SaveOutlineService saveOutlineService;


    @PostMapping("/saveOutline")
    public Map<String, Long> SaveOutline(@RequestBody OutlineDTO outlineDTO) {
        log.info("接收到的大纲：{}",outlineDTO);
        return Collections.singletonMap("novelId", saveOutlineService.saveOutline(outlineDTO));
    }
}
