package com.novelgeneration.novel.service;

import com.novelgeneration.novel.dto.StoryboardGenerationContext;

public interface StoryboardGenerationPreparationService {
    StoryboardGenerationContext prepare(Long novelId, Long chapterNum, Long sceneId, String promptOverride);

    String toProviderImagePath(String path);
}
