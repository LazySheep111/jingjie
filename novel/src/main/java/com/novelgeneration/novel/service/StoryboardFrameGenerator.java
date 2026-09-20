package com.novelgeneration.novel.service;

import com.novelgeneration.novel.dto.StoryboardFrameRequest;

public interface StoryboardFrameGenerator {
    String generate(StoryboardFrameRequest request);
}
