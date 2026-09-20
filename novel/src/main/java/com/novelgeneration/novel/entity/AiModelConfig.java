package com.novelgeneration.novel.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiModelConfig {
    private Long id;
    private String capabilityType;
    private String providerType;
    private String apiUrl;
    private String queryUrl;
    private String encryptedApiKey;
    private String modelName;
    private Boolean enabled;
    private String testStatus;
    private LocalDateTime lastTestAt;
    private String lastError;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
