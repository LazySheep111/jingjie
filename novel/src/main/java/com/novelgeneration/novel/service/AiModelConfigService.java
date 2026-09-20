package com.novelgeneration.novel.service;

import com.novelgeneration.novel.dto.AiModelConfigRequest;
import com.novelgeneration.novel.vo.AiModelConfigTestResult;
import com.novelgeneration.novel.vo.AiModelConfigVO;

import java.util.List;

public interface AiModelConfigService {
    List<AiModelConfigVO> list();

    AiModelConfigTestResult test(String capabilityType, AiModelConfigRequest request);

    AiModelConfigVO save(String capabilityType, AiModelConfigRequest request);

    AiModelConfigVO disable(String capabilityType);
}
