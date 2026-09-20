package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.mapper.ChapterMapper;
import com.novelgeneration.novel.service.ChapterService;
import com.novelgeneration.novel.vo.ChapterVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class ChapterServiceImpl implements ChapterService {

    @Resource
    private ChapterMapper chapterMapper;

    @Override
    public List<ChapterVO> selectChapter(Long novelId) {
        return chapterMapper.selectChapter(novelId);
    }
}
