package com.novelgeneration.novel.service;

import com.novelgeneration.novel.vo.NovelHistoryDetailVO;
import com.novelgeneration.novel.vo.NovelHistoryItemVO;

public interface HistoryService {
    NovelHistoryDetailVO.PageResult<NovelHistoryItemVO> listHistory(Integer page, Integer pageSize);

    NovelHistoryDetailVO getOutline(Long novelId);

    void deleteNovel(Long novelId);
}
