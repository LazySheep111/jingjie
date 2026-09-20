package com.novelgeneration.novel.service;

import com.novelgeneration.novel.dto.AssetExtractRequest;
import com.novelgeneration.novel.vo.AssetTaskVO;
import com.novelgeneration.novel.vo.VisualAssetVO;

import java.util.List;

public interface AssetExtractionService {
    AssetTaskVO start(Long novelId, Long chapterNum, AssetExtractRequest request);

    AssetTaskVO getStatus(String taskId);

    void execute(String taskId);

    List<VisualAssetVO> parseEntities(String aiResponse);

    VisualAssetVO generatePrompts(VisualAssetVO entity);
}
