package com.novelgeneration.novel.service;

import com.novelgeneration.novel.dto.OutlineDTO;
import com.novelgeneration.novel.dto.Result;

public interface GenerateFullService {
    Result GenerateFull(Long novelId,OutlineDTO outlineDTO);
}
