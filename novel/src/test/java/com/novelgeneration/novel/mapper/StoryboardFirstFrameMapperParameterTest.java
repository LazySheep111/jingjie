package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.vo.StoryboardFirstFrameVO;
import org.apache.ibatis.annotations.Param;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryboardFirstFrameMapperParameterTest {
    @Test
    void multiParameterMethodsUseExplicitParameterNames() {
        for (Method method : StoryboardFirstFrameMapper.class.getDeclaredMethods()) {
            if (method.getParameterCount() < 2) {
                continue;
            }
            for (java.lang.reflect.Parameter parameter : method.getParameters()) {
                assertTrue(parameter.isAnnotationPresent(Param.class),
                        method.getName() + " must declare @Param on every SQL parameter");
            }
        }
    }

    @Test
    void firstFrameVoCarriesVersionStatusAndDeletionState() throws Exception {
        assertNotNull(StoryboardFirstFrameVO.class.getDeclaredField("version"));
        assertNotNull(StoryboardFirstFrameVO.class.getDeclaredField("status"));
        assertNotNull(StoryboardFirstFrameVO.class.getDeclaredField("deleted"));
        assertNotNull(StoryboardFirstFrameVO.class.getDeclaredField("source"));
        assertTrue(StoryboardFirstFrameMapper.COLUMNS.contains("source AS source"));
        Method method = StoryboardFirstFrameMapper.class.getMethod("nextVersion", Long.class);
        assertEquals(Integer.class, method.getReturnType());
    }
}
