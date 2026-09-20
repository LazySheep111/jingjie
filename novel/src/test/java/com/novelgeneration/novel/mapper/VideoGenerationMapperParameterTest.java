package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.vo.StoryboardVideoVO;
import com.novelgeneration.novel.vo.VideoGenerationTaskVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoGenerationMapperParameterTest {
    @Test
    void taskAndVideoExposeFirstFramePath() {
        VideoGenerationTaskVO task = new VideoGenerationTaskVO();
        task.setFirstFrameId(9L);
        task.setFirstFramePath("/api/storyboard-frames/files/1/chapter-1/scene-1/frame.png");
        StoryboardVideoVO video = new StoryboardVideoVO();
        video.setFirstFramePath(task.getFirstFramePath());

        assertEquals(task.getFirstFramePath(), video.getFirstFramePath());
        assertEquals(9L, task.getFirstFrameId());
    }

    @Test
    void updateFirstFrameNamesBothMyBatisParameters() throws Exception {
        Method method = VideoGenerationTaskMapper.class.getMethod(
                "updateFirstFrame", Long.class, String.class);

        assertEquals("taskId", method.getParameters()[0].getAnnotation(Param.class).value());
        assertEquals("firstFramePath", method.getParameters()[1].getAnnotation(Param.class).value());
    }

    @Test
    void runningQueryOnlyIncludesVideoGenerationStages() throws Exception {
        Method method = VideoGenerationTaskMapper.class.getMethod(
                "selectRunning", Long.class, Long.class, Long.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value());

        org.junit.jupiter.api.Assertions.assertTrue(sql.contains("GENERATING_VIDEO"));
        org.junit.jupiter.api.Assertions.assertFalse(sql.contains("GENERATING_FRAME"));
    }

    @Test
    void videoVersionsExposeSourceDeletionAndFallbackQueries() throws Exception {
        assertNotNull(StoryboardVideoVO.class.getDeclaredField("source"));
        assertNotNull(StoryboardVideoVO.class.getDeclaredField("deleted"));
        assertTrue(StoryboardVideoMapper.COLUMNS.contains("source AS source"));
        assertTrue(StoryboardVideoMapper.COLUMNS.contains("is_deleted AS deleted"));
        assertNotNull(StoryboardVideoMapper.class.getMethod("softDelete", Long.class));
        assertNotNull(StoryboardVideoMapper.class.getMethod("selectLatestAvailableByScene", Long.class));
    }

    @Test
    void videoHistoryExposesResolution() throws Exception {
        assertNotNull(StoryboardVideoVO.class.getDeclaredField("resolution"));
        assertTrue(StoryboardVideoMapper.COLUMNS.contains("resolution AS resolution"));
        StoryboardVideoVO video = new StoryboardVideoVO();
        video.setResolution("768P");
        assertEquals("768P", video.getResolution());
    }
}
