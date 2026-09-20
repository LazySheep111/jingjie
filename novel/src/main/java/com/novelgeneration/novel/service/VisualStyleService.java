package com.novelgeneration.novel.service;

import com.novelgeneration.novel.vo.VisualStyleVO;

public interface VisualStyleService {
    VisualStyleVO get(Long novelId);

    VisualStyleVO save(Long novelId, VisualStyleVO request);

    VisualStyleVO generateInitial(Long novelId);

    String buildPrompt(Long novelId);
}
