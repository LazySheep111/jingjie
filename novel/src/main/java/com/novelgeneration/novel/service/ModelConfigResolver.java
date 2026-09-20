package com.novelgeneration.novel.service;

import com.novelgeneration.novel.vo.AiModelConfigSnapshot;

public interface ModelConfigResolver {
    AiModelConfigSnapshot resolveText();

    AiModelConfigSnapshot resolveImage();

    AiModelConfigSnapshot resolveVideo();
}
