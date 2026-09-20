package com.novelgeneration.novel.service;

import com.novelgeneration.novel.vo.ChapterVO;

import java.util.List;

public interface ChapterService {
    List<ChapterVO> selectChapter(Long novelId);
}
