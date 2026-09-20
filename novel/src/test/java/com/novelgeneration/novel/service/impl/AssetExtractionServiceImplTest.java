package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.mapper.ChapterMapper;
import com.novelgeneration.novel.mapper.StoryboardMapper;
import com.novelgeneration.novel.mapper.VisualAssetMapper;
import com.novelgeneration.novel.service.VisualAssetService;
import com.novelgeneration.novel.service.VisualStyleService;
import com.novelgeneration.novel.utils.AiUtil;
import com.novelgeneration.novel.vo.AssetTaskVO;
import com.novelgeneration.novel.vo.ChapterVO;
import com.novelgeneration.novel.vo.VisualAssetVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetExtractionServiceImplTest {

    @Mock
    private VisualAssetMapper visualAssetMapper;
    @Mock
    private VisualAssetService visualAssetService;
    @Mock
    private ChapterMapper chapterMapper;
    @Mock
    private StoryboardMapper storyboardMapper;
    @Mock
    private AiUtil aiUtil;
    @Mock
    private VisualStyleService visualStyleService;

    @InjectMocks
    private AssetExtractionServiceImpl service;

    @Test
    void parseEntities_acceptsStrictEntityJson() throws Exception {
        service.objectMapper = new ObjectMapper();
        List<VisualAssetVO> result = service.parseEntities("{\"entities\":[{\"assetType\":\"CHARACTER\",\"assetName\":\"凌程\",\"coreFeatures\":\"黑发\"}]}");

        assertEquals(1, result.size());
        assertEquals("凌程", result.get(0).getAssetName());
        assertEquals("CHARACTER", result.get(0).getAssetType());
    }

    @Test
    void parseEntities_rejectsEmptyEntities() {
        service.objectMapper = new ObjectMapper();

        assertThrows(IllegalArgumentException.class,
                () -> service.parseEntities("{\"entities\":[]}"));
    }

    @Test
    void parseEntities_rejectsWrongSchemaTypeBeforeMapping() {
        service.objectMapper = new ObjectMapper();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.parseEntities("{\"entities\":[{\"assetType\":1,\"assetName\":\"凌程\",\"coreFeatures\":\"黑发\"}]}"));

        assertTrue(error.getCause().getMessage().contains("Structured output validation failed"));
    }

    @Test
    void generatePrompts_requiresAllThreeViews() {
        service.objectMapper = new ObjectMapper();

        assertThrows(IllegalArgumentException.class,
                () -> service.generatePrompts(entity()));
    }

    @Test
    void generatePrompts_requiresChinesePromptContent() {
        service.objectMapper = new ObjectMapper();
        when(aiUtil.chatJsonObject(anyString())).thenReturn("{\"frontPrompt\":\"正面\",\"sidePrompt\":\"侧面\",\"backPrompt\":\"背面\"}");

        service.generatePrompts(entity());

        verify(aiUtil).chatJsonObject(argThat(prompt -> prompt.contains("三个字段的内容必须全部使用中文")));
    }

    @Test
    void start_resubmitsPendingTask() {
        AssetTaskVO pending = new AssetTaskVO();
        pending.setTaskId("stale-task");
        pending.setNovelId(53L);
        pending.setChapterNum(1L);
        pending.setStatus("PENDING");
        when(visualAssetMapper.selectRunningTask(53L, 1L)).thenReturn(pending);

        service.start(53L, 1L, new com.novelgeneration.novel.dto.AssetExtractRequest());

        verify(visualAssetMapper, timeout(1000)).claimTask(anyString());
    }

    @Test
    void execute_marksTaskFailedWhenInitialProgressUpdateFails() {
        service.objectMapper = new ObjectMapper();
        AssetTaskVO task = task("pre-processing-failure", "PENDING");
        when(visualAssetMapper.claimTask(task.getTaskId())).thenReturn(1);
        when(visualAssetMapper.selectTask(task.getTaskId())).thenReturn(task);
        when(visualAssetMapper.updateTask(task))
                .thenThrow(new IllegalStateException("progress update failed"))
                .thenReturn(1);

        service.execute(task.getTaskId());

        verify(visualAssetMapper, times(2)).updateTask(task);
        org.junit.jupiter.api.Assertions.assertEquals("FAILED", task.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("progress update failed", task.getErrorMessage());
    }

    @Test
    void start_requeuesAndResubmitsStaleProcessingTask() {
        AssetTaskVO stale = task("stale-processing", "PROCESSING");
        stale.setUpdateTime(LocalDateTime.now().minusMinutes(20));
        when(visualAssetMapper.selectRunningTask(53L, 1L)).thenReturn(stale);
        when(visualAssetMapper.resetStaleTask(stale.getTaskId(), 600)).thenReturn(1);

        service.start(53L, 1L, new com.novelgeneration.novel.dto.AssetExtractRequest());

        verify(visualAssetMapper).resetStaleTask(stale.getTaskId(), 600);
        verify(visualAssetMapper, timeout(1000)).claimTask(stale.getTaskId());
    }

    @Test
    void start_doesNotResubmitFreshProcessingTask() {
        AssetTaskVO fresh = task("fresh-processing", "PROCESSING");
        fresh.setUpdateTime(LocalDateTime.now().minusMinutes(1));
        when(visualAssetMapper.selectRunningTask(53L, 1L)).thenReturn(fresh);

        service.start(53L, 1L, new com.novelgeneration.novel.dto.AssetExtractRequest());

        verify(visualAssetMapper, never()).resetStaleTask(anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void execute_repairsTruncatedEntityJson() {
        service.objectMapper = new ObjectMapper();

        AssetTaskVO task = new AssetTaskVO();
        task.setTaskId("repair-task");
        task.setNovelId(53L);
        task.setChapterNum(1L);
        task.setStatus("PENDING");
        task.setTotal(0);
        task.setReused(0);
        task.setCreated(0);
        task.setFailed(0);
        ChapterVO chapter = new ChapterVO();
        chapter.setChapterTitle("第一章");
        chapter.setChapterText("凌程站在青石河边。");
        VisualAssetVO asset = entity();
        asset.setAssetId(1L);
        asset.setVersion(1);
        asset.setStatus("PROMPT_READY");

        when(visualAssetMapper.claimTask("repair-task")).thenReturn(1);
        when(visualAssetMapper.selectTask("repair-task")).thenReturn(task);
        when(chapterMapper.selectChapterByNum(53L, 1L)).thenReturn(chapter);
        when(aiUtil.chatJsonObject(anyString())).thenReturn("{\"entities\":[{\"assetType\":\"CHARACTER\"")
                .thenReturn("{\"entities\":[{\"assetType\":\"CHARACTER\",\"assetName\":\"凌程\",\"coreFeatures\":\"少年\"}]}");
        when(visualAssetService.findOrCreate(53L, "CHARACTER", "凌程", "凌程", "少年"))
                .thenReturn(asset);

        service.execute("repair-task");

        verify(aiUtil, times(2)).chatJsonObject(anyString());
        verify(visualAssetService).findOrCreate(53L, "CHARACTER", "凌程", "凌程", "少年");
    }

    @Test
    void executePassesTaskNovelIdToPromptGeneration() {
        service.objectMapper = new ObjectMapper();

        AssetTaskVO task = new AssetTaskVO();
        task.setTaskId("prompt-task");
        task.setNovelId(53L);
        task.setChapterNum(1L);
        task.setStatus("PENDING");
        task.setTotal(0);
        task.setReused(0);
        task.setCreated(0);
        task.setFailed(0);

        ChapterVO chapter = new ChapterVO();
        chapter.setNovelId(53L);
        chapter.setChapterTitle("第一章");
        chapter.setChapterText("凌程站在青石河边。");

        VisualAssetVO asset = entity();
        asset.setAssetId(1L);
        asset.setVersion(1);
        asset.setStatus("PROMPT_PENDING");

        when(visualAssetMapper.claimTask("prompt-task")).thenReturn(1);
        when(visualAssetMapper.selectTask("prompt-task")).thenReturn(task);
        when(chapterMapper.selectChapterByNum(53L, 1L)).thenReturn(chapter);
        when(aiUtil.chatJsonObject(anyString()))
                .thenReturn("{\"entities\":[{\"assetType\":\"CHARACTER\",\"assetName\":\"凌程\",\"coreFeatures\":\"少年\"}]}")
                .thenReturn("{\"frontPrompt\":\"正面\",\"sidePrompt\":\"侧面\",\"backPrompt\":\"背面\"}");
        when(visualAssetService.findOrCreate(53L, "CHARACTER", "凌程", "凌程", "少年"))
                .thenReturn(asset);
        when(visualStyleService.buildPrompt(53L)).thenReturn("统一视觉风格");

        service.execute("prompt-task");

        verify(visualStyleService, times(2)).buildPrompt(53L);
    }

    private VisualAssetVO entity() {
        VisualAssetVO entity = new VisualAssetVO();
        entity.setAssetType("CHARACTER");
        entity.setAssetName("凌程");
        entity.setCoreFeatures("黑发");
        return entity;
    }

    private AssetTaskVO task(String taskId, String status) {
        AssetTaskVO task = new AssetTaskVO();
        task.setTaskId(taskId);
        task.setNovelId(53L);
        task.setChapterNum(1L);
        task.setStatus(status);
        task.setTotal(0);
        task.setReused(0);
        task.setCreated(0);
        task.setFailed(0);
        return task;
    }
}
