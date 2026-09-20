package com.novelgeneration.novel.service;

import com.novelgeneration.novel.dto.StoryboardGenerateRequest;
import com.novelgeneration.novel.vo.StoryboardVO;

public interface StoryboardService {
    StoryboardVO generate(Long novelId, StoryboardGenerateRequest request);

    StoryboardVO getLatest(Long novelId, Long chapterNum);

    StoryboardVO update(Long novelId, Long chapterNum, StoryboardVO storyboard);

    void removeAssetReference(Long novelId, Long chapterNum, Long sceneId, Long assetId, String assetRole);

    String exportText(Long novelId, Long chapterNum);
}
