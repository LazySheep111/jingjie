package com.novelgeneration.novel.service;

import com.novelgeneration.novel.dto.VideoProviderRequest;
import lombok.Data;

public interface VideoGenerationClient {
    ProviderTask submit(VideoProviderRequest request);

    ProviderTask query(String providerTaskId);

    @Data
    class ProviderTask {
        private String taskId;
        private String status;
        private String videoUrl;
        private String errorMessage;
    }
}
