package com.novelgeneration.novel.dto;

import lombok.Data;

@Data
public class AiModelConfigRequest {
    private String providerType;
    private String apiUrl;
    private String queryUrl;
    private String apiKey;
    private String modelName;
}
