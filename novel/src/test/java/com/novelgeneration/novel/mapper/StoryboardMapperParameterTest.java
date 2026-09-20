package com.novelgeneration.novel.mapper;

import org.apache.ibatis.annotations.Param;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryboardMapperParameterTest {

    @Test
    void namesAllParametersUsedByStoryboardSql() throws NoSuchMethodException {
        assertHasNamedParameters("selectLatest", 2);
        assertHasNamedParameters("nextVersion", 2);
        assertHasNamedParameters("deleteScenesByNovelAndChapter", 2);
        assertHasNamedParameters("deleteByNovelAndChapter", 2);
        assertHasNamedParameters("selectAssetIds", 2);
        assertHasNamedParameters("deleteAssetReference", 5);
    }

    private void assertHasNamedParameters(String methodName, int parameterCount) throws NoSuchMethodException {
        Method method = Arrays.stream(StoryboardMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        assertTrue(Arrays.stream(method.getParameters())
                        .limit(parameterCount)
                        .allMatch(parameter -> parameter.getAnnotation(Param.class) != null),
                methodName + " must declare @Param on every SQL parameter");
    }
}
