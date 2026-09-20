package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.mapper.CompositeImageTaskMapper;
import com.novelgeneration.novel.mapper.VisualAssetMapper;
import com.novelgeneration.novel.service.VisualStyleService;
import com.novelgeneration.novel.vo.VisualAssetVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CompositeImageServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void generateAndDownload_submitsReferenceImageAlongsidePrompt() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        CompositeImageServiceImpl service = new CompositeImageServiceImpl();
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "apiUrl", "https://image.test/v1");
        ReflectionTestUtils.setField(service, "apiModel", "test-model");
        ReflectionTestUtils.setField(service, "localDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "referenceImageUrl", "https://cdn.test/three-view-reference.jpg");
        VisualStyleService visualStyleService = mock(VisualStyleService.class);
        when(visualStyleService.buildPrompt(53L)).thenReturn("统一视觉风格");
        ReflectionTestUtils.setField(service, "visualStyleService", visualStyleService);

        server.expect(requestTo("https://image.test/v1/images/generations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"image\":[\"https://cdn.test/three-view-reference.jpg\"]")))
                .andRespond(withSuccess("{\"data\":[{\"b64_json\":\"AQID\"}]}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        VisualAssetVO asset = new VisualAssetVO();
        asset.setAssetId(53L);
        asset.setNovelId(53L);
        asset.setVersion(1);
        asset.setFrontPrompt("正面");
        asset.setSidePrompt("侧面");
        asset.setBackPrompt("背面");

        ReflectionTestUtils.invokeMethod(service, "generateAndDownload", asset);

        server.verify();
    }

    @Test
    void generateAndDownload_worksWithoutReferenceImage() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        CompositeImageServiceImpl service = new CompositeImageServiceImpl();
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "apiUrl", "https://image.test/v1/images/generations");
        ReflectionTestUtils.setField(service, "apiModel", "test-model");
        ReflectionTestUtils.setField(service, "localDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "referenceImageUrl", "");
        VisualStyleService visualStyleService = mock(VisualStyleService.class);
        when(visualStyleService.buildPrompt(53L)).thenReturn("统一视觉风格");
        ReflectionTestUtils.setField(service, "visualStyleService", visualStyleService);

        server.expect(requestTo("https://image.test/v1/images/generations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(not(containsString("\"image\""))))
                .andRespond(withSuccess("{\"data\":[{\"b64_json\":\"AQID\"}]}",
                        org.springframework.http.MediaType.APPLICATION_JSON));

        VisualAssetVO asset = new VisualAssetVO();
        asset.setAssetId(53L);
        asset.setNovelId(53L);
        asset.setVersion(1);
        asset.setFrontPrompt("正面");
        asset.setSidePrompt("侧面");
        asset.setBackPrompt("背面");

        ReflectionTestUtils.invokeMethod(service, "generateAndDownload", asset);

        server.verify();
    }
}
