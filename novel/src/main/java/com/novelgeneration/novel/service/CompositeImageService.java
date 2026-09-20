package com.novelgeneration.novel.service;

import com.novelgeneration.novel.vo.CompositeImageTaskVO;
import com.novelgeneration.novel.vo.VisualAssetVO;

public interface CompositeImageService {
    CompositeImageTaskVO start(Long novelId, Long assetId, VisualAssetVO request);

    CompositeImageTaskVO getStatus(String taskId);
}
