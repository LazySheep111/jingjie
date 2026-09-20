package com.novelgeneration.novel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiModelConfigTestResult {
    private boolean success;
    private String status;
    private String message;

    public static AiModelConfigTestResult success(String message) {
        return new AiModelConfigTestResult(true, "PASSED", message);
    }

    public static AiModelConfigTestResult failure(String message) {
        return new AiModelConfigTestResult(false, "FAILED", message);
    }
}
