package com.novelgeneration.novel.service;

import com.novelgeneration.novel.vo.AssistantMessageVO;

public interface AssistantMessageEventPublisher {
    void publish(AssistantMessageVO message);
}
