package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.dto.StoryboardFrameRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ConfiguredStoryboardFrameGeneratorTest {
    @TempDir
    Path tempDir;

    @Test
    void generateSubmitsReferencesAndStoresOneCinematicFrame() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ConfiguredStoryboardFrameGenerator generator = new ConfiguredStoryboardFrameGenerator();
        ReflectionTestUtils.setField(generator, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(generator, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(generator, "apiKey", "test-key");
        ReflectionTestUtils.setField(generator, "apiUrl", "https://image.test/v1");
        ReflectionTestUtils.setField(generator, "apiModel", "seedream-test");
        ReflectionTestUtils.setField(generator, "frameDir", tempDir.toString());

        server.expect(requestTo("https://image.test/v1/images/generations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"model\":\"seedream-test\"")))
                .andExpect(content().string(containsString("单张16:9完整影视画面")))
                .andExpect(content().string(containsString("https://cdn.test/person.png")))
                .andExpect(content().string(containsString("https://cdn.test/location.png")))
                .andExpect(content().string(containsString("\"size\":\"2K\"")))
                .andExpect(content().string(containsString("\"response_format\":\"b64_json\"")))
                .andRespond(withSuccess("{\"data\":[{\"b64_json\":\"AQID\"}]}", MediaType.APPLICATION_JSON));

        StoryboardFrameRequest request = new StoryboardFrameRequest();
        request.setNovelId(53L);
        request.setChapterNum(1L);
        request.setSceneId(7L);
        request.setPrompt("请生成一张单张16:9完整影视画面，作为分镜第一帧。");
        request.setReferenceImageUrls(List.of(
                "https://cdn.test/person.png", "https://cdn.test/location.png"));

        String path = generator.generate(request);

        assertTrue(path.startsWith("/api/storyboard-frames/files/53/chapter-1/scene-7/frame-"));
        assertTrue(Files.list(tempDir.resolve("53/chapter-1/scene-7")).findAny().isPresent());
        server.verify();
    }
}
