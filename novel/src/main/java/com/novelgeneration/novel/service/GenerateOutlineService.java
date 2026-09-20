package com.novelgeneration.novel.service;

import com.novelgeneration.novel.dto.KeysDTO;
import com.novelgeneration.novel.dto.OutlineDTO;
import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.vo.NovelOutlineVO;

public interface GenerateOutlineService{
    NovelOutlineVO generateOutline(KeysDTO keysDTO);

}
