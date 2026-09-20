package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.service.AiModelConfigService;
import com.novelgeneration.novel.vo.AiModelConfigVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiModelConfigControllerTest {
    @Mock
    private AiModelConfigService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(newController()).build();
    }

    @Test
    void listResponseContainsMaskedKeyFieldAndNoRawKeyField() throws Exception {
        AiModelConfigVO vo = new AiModelConfigVO();
        vo.setCapabilityType("TEXT");
        vo.setApiKeyMasked("sk-****alue");
        when(service.list()).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/model-configs"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("apiKeyMasked")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("apiKey\\\""))));
    }

    private AiModelConfigController newController() {
        AiModelConfigController controller = new AiModelConfigController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "service", service);
        return controller;
    }
}
