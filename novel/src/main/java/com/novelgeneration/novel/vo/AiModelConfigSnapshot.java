package com.novelgeneration.novel.vo;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class AiModelConfigSnapshot {
    private String capabilityType;
    private String providerType;
    private String apiUrl;
    private String queryUrl;
    private String apiKey;
    private String modelName;
    private boolean enabled;
    private boolean databaseSource;
}
