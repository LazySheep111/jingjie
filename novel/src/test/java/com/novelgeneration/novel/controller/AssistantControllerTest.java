package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.AssistantChatRequest;
import com.novelgeneration.novel.dto.AssistantChatResponse;
import com.novelgeneration.novel.service.AssistantService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssistantControllerTest {

    @Test
    void chatReturnsReadOnlyAssistantResponse() {
        AssistantService service = mock(AssistantService.class);
        AssistantController controller = new AssistantController();
        ReflectionTestUtils.setField(controller, "assistantService", service);
        AssistantChatRequest request = new AssistantChatRequest();
        request.setMessage("当前作品叫什么？");
        when(service.chat(request)).thenReturn(new AssistantChatResponse("绝境逆袭"));

        var result = controller.chat(request);

        assertEquals(true, result.getSuccess());
        assertEquals("绝境逆袭", ((AssistantChatResponse) result.getData()).getAnswer());
    }
}
