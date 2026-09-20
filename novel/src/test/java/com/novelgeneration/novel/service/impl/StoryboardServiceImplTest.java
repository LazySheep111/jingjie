package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.dto.StoryboardGenerateRequest;
import com.novelgeneration.novel.mapper.StoryboardMapper;
import com.novelgeneration.novel.utils.AiUtil;
import com.novelgeneration.novel.service.VisualStyleService;
import com.novelgeneration.novel.vo.StoryboardVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoryboardServiceImplTest {

    @Test
    void generatesParsesAndSavesStoryboardScenes() {
        StoryboardMapper mapper = mock(StoryboardMapper.class);
        AiUtil aiUtil = mock(AiUtil.class);
        VisualStyleService visualStyleService = mock(VisualStyleService.class);
        StoryboardServiceImpl service = new StoryboardServiceImpl();
        ReflectionTestUtils.setField(service, "storyboardMapper", mapper);
        ReflectionTestUtils.setField(service, "aiUtil", aiUtil);
        ReflectionTestUtils.setField(service, "visualStyleService", visualStyleService);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        when(mapper.nextVersion(50L, 1L)).thenReturn(1);
        doAnswer(invocation -> {
            StoryboardVO storyboard = invocation.getArgument(0);
            storyboard.setId(900L);
            return 1;
        }).when(mapper).insertStoryboard(any(StoryboardVO.class));

        when(aiUtil.chatJsonObject(any(String.class))).thenReturn("""
                {
                  "totalDurationSec": 20,
                  "scenes": [
                    {
                      "sequence": 1,
                      "durationSec": 10,
                      "location": "河边",
                      "shotPlan": "时长 0:00-0:05\\n[0:00-0:05 | 全景]：少年站在河水中。"
                    },
                    {
                      "sequence": 2,
                      "durationSec": 10,
                      "location": "河边",
                      "shotPlan": "时长 0:00-0:05\\n[0:00-0:03 | 特写]：水底出现神秘光芒。\\n[0:03-0:05 | 近景]：少年低头看向水底。"
                    }
                  ]
                }
                """);

        StoryboardGenerateRequest request = new StoryboardGenerateRequest();
        request.setNovelId(50L);
        request.setChapterNum(1L);
        request.setChapterTitle("河底奇遇");
        request.setChapterText("少年在河边发现神秘光芒。");
        when(visualStyleService.buildPrompt(50L)).thenReturn("时代背景：古代山村；禁止出现：现代建筑");

        StoryboardVO result = service.generate(50L, request);

        assertEquals(50L, result.getNovelId());
        assertEquals(1L, result.getChapterNum());
        assertEquals(2, result.getScenes().size());
        assertEquals("时长 0:00-0:05\n[0:00-0:03 | 特写]：水底出现神秘光芒。\n[0:03-0:05 | 近景]：少年低头看向水底。", result.getScenes().get(1).getShotPlan());
        verify(aiUtil).chatJsonObject(org.mockito.ArgumentMatchers.contains("时代背景：古代山村"));
        verify(mapper, times(1)).insertStoryboard(any(StoryboardVO.class));
        verify(mapper, times(2)).insertScene(eq(900L), eq(1L), any(StoryboardVO.Scene.class));
    }

    @Test
    void updateRecalculatesTotalDurationFromUserEditedSceneDurations() {
        StoryboardMapper mapper = mock(StoryboardMapper.class);
        StoryboardServiceImpl service = new StoryboardServiceImpl();
        ReflectionTestUtils.setField(service, "storyboardMapper", mapper);

        StoryboardVO saved = new StoryboardVO();
        saved.setId(900L);
        saved.setTotalDurationSec(20);
        StoryboardVO.Scene first = new StoryboardVO.Scene();
        first.setId(1L);
        first.setShotPlan("时长 0:00-0:04\n[0:00-0:04 | 全景]：第一镜头。");
        StoryboardVO.Scene second = new StoryboardVO.Scene();
        second.setId(2L);
        second.setShotPlan("时长 0:00-0:06\n[0:00-0:06 | 中景]：第二镜头。");
        when(mapper.selectLatest(50L, 1L)).thenReturn(saved);
        when(mapper.selectScenes(900L)).thenReturn(java.util.List.of(first, second));

        StoryboardVO edited = new StoryboardVO();
        edited.setScenes(java.util.List.of(first, second));

        StoryboardVO result = service.update(50L, 1L, edited);

        assertEquals(10, result.getTotalDurationSec());
    }

    @Test
    void removesOnlyTheCurrentSceneAssetReference() {
        StoryboardMapper mapper = mock(StoryboardMapper.class);
        StoryboardServiceImpl service = new StoryboardServiceImpl();
        ReflectionTestUtils.setField(service, "storyboardMapper", mapper);
        when(mapper.deleteAssetReference(1L, 2L, 3L, 4L, "CHARACTER")).thenReturn(1);

        service.removeAssetReference(1L, 2L, 3L, 4L, "CHARACTER");

        verify(mapper).deleteAssetReference(1L, 2L, 3L, 4L, "CHARACTER");
    }

    @Test
    void rejectsUnsupportedAssetRoleWhenRemovingReference() {
        StoryboardServiceImpl service = new StoryboardServiceImpl();

        assertThrows(IllegalArgumentException.class,
                () -> service.removeAssetReference(1L, 2L, 3L, 4L, "OTHER"));
    }

    @Test
    void exportsScenesAsAccumulatedTimelineLines() {
        StoryboardMapper mapper = mock(StoryboardMapper.class);
        StoryboardServiceImpl service = new StoryboardServiceImpl();
        ReflectionTestUtils.setField(service, "storyboardMapper", mapper);
        StoryboardVO storyboard = new StoryboardVO();
        StoryboardVO.Scene first = new StoryboardVO.Scene();
        first.setSequence(1);
        first.setDurationSec(5);
        first.setShotPlan("时长 0:00-0:05\n[0:00-0:05 | 全景]：人物站在河边。");
        StoryboardVO.Scene second = new StoryboardVO.Scene();
        second.setSequence(2);
        second.setDurationSec(10);
        second.setShotPlan("时长 0:00-0:10\n[0:00-0:05 | 中景 -> 推进]：镜头靠近人物。\n台词（人物，坚定）：我们走");
        storyboard.setScenes(java.util.List.of(first, second));
        when(mapper.selectLatest(50L, 1L)).thenReturn(storyboard);
        when(mapper.selectScenes(null)).thenReturn(storyboard.getScenes());

        String exported = service.exportText(50L, 1L);

        assertTrue(exported.contains("时长 0:00-0:05\n[0:00-0:05 | 全景]：人物站在河边。"));
        assertTrue(exported.contains("时长 0:00-0:10\n[0:00-0:05 | 中景 -> 推进]：镜头靠近人物。"));
        assertTrue(exported.contains("台词（人物，坚定）：我们走"));
    }

    @Test
    void generationPromptRequiresTimelineFriendlyStoryboardFields() {
        StoryboardMapper mapper = mock(StoryboardMapper.class);
        AiUtil aiUtil = mock(AiUtil.class);
        VisualStyleService visualStyleService = mock(VisualStyleService.class);
        StoryboardServiceImpl service = new StoryboardServiceImpl();
        ReflectionTestUtils.setField(service, "storyboardMapper", mapper);
        ReflectionTestUtils.setField(service, "aiUtil", aiUtil);
        ReflectionTestUtils.setField(service, "visualStyleService", visualStyleService);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        when(mapper.nextVersion(50L, 1L)).thenReturn(1);
        when(aiUtil.chatJsonObject(any(String.class))).thenReturn("{\"scenes\":[{\"sequence\":1,\"shotPlan\":\"时长 0:00-0:04\\n[0:00-0:04 | 全景]：画面\"}]}" );
        when(visualStyleService.buildPrompt(50L)).thenReturn("");

        StoryboardGenerateRequest request = new StoryboardGenerateRequest();
        request.setChapterNum(1L);
        request.setChapterText("正文");
        service.generate(50L, request);

        verify(aiUtil).chatJsonObject(argThat((String prompt) -> prompt.contains("时间轴")
                && prompt.contains("shotPlan")
                && prompt.contains("[0:00-0:02 | 远景 -> 全景]")));
    }

    @Test
    void rejectsStoryboardWhenSceneSequenceHasWrongTypeBeforeSaving() {
        StoryboardMapper mapper = mock(StoryboardMapper.class);
        AiUtil aiUtil = mock(AiUtil.class);
        VisualStyleService visualStyleService = mock(VisualStyleService.class);
        StoryboardServiceImpl service = new StoryboardServiceImpl();
        ReflectionTestUtils.setField(service, "storyboardMapper", mapper);
        ReflectionTestUtils.setField(service, "aiUtil", aiUtil);
        ReflectionTestUtils.setField(service, "visualStyleService", visualStyleService);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        when(aiUtil.chatJsonObject(any(String.class))).thenReturn("""
                {
                  "totalDurationSec": 4,
                  "scenes": [
                    {
                      "sequence": "1",
                      "durationSec": 4,
                      "location": "河边",
                      "timeOfDay": "白天",
                      "weather": "晴",
                      "characters": "少年",
                      "shotType": "全景",
                      "cameraMovement": "固定",
                      "shotPlan": "时长 0:00-0:04\\n[0:00-0:04 | 全景]：少年站在河边。",
                      "characterEmotion": "紧张",
                      "voiceOver": "",
                      "transition": "切入",
                      "imagePrompt": "河边少年"
                    }
                  ]
                }
                """);

        StoryboardGenerateRequest request = new StoryboardGenerateRequest();
        request.setNovelId(50L);
        request.setChapterNum(1L);
        request.setChapterText("正文");
        when(visualStyleService.buildPrompt(50L)).thenReturn("");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.generate(50L, request));

        assertTrue(error.getCause().getMessage().contains("Structured output validation failed"));
        verify(mapper, times(0)).insertStoryboard(any(StoryboardVO.class));
    }
}
