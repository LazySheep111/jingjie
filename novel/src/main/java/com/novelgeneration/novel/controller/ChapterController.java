package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.dto.StatusDTO;
import com.novelgeneration.novel.service.ChapterService;
import com.novelgeneration.novel.vo.ChapterVO;
import com.novelgeneration.novel.vo.StatusVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RequestMapping({"/api/novel"})
@RestController
public class ChapterController {

    @Resource
    private ChapterService chapterService;

    @GetMapping("/{novelId}/chapters")
    public Result chapter(@PathVariable Long novelId){
        List<ChapterVO> chapterVOList = chapterService.selectChapter(novelId);
        return Result.ok(chapterVOList);
    }
}
