package com.novelgeneration.novel.service;

import com.novelgeneration.novel.dto.OutlineDTO;
import com.novelgeneration.novel.vo.NovelOutlineVO;

public interface GenerateOutlineService{
    NovelOutlineVO generateOutline(OutlineDTO outlineDTO);
}
