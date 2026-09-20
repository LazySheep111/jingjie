package com.novelgeneration.novel.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualAssetMapperParameterTest {

    @Test
    void namesAllMultiParameterMapperArguments() {
        Arrays.stream(VisualAssetMapper.class.getDeclaredMethods())
                .filter(method -> method.getParameterCount() > 1)
                .forEach(this::assertAllParametersNamed);
    }

    @Test
    void chapterAssetQueryOnlyUsesParametersDeclaredByItsMethod() throws NoSuchMethodException {
        Method method = VisualAssetMapper.class.getMethod("selectAssetsByChapter", Long.class, Long.class);
        String sql = method.getAnnotation(Select.class).value()[0];

        assertTrue(!sql.contains("#{version}"),
                "selectAssetsByChapter must not reference an undeclared version parameter");
    }

    @Test
    void sceneAssetReferenceInsertIsIdempotent() throws NoSuchMethodException {
        Method method = VisualAssetMapper.class.getMethod("insertSceneAssetRef",
                Long.class, Long.class, Integer.class, String.class);
        String sql = method.getAnnotation(Insert.class).value()[0];

        assertTrue(sql.toUpperCase().contains("INSERT IGNORE"),
                "repeated asset extraction must not fail on an existing scene reference");
    }

    @Test
    void assetTaskQueriesPersistProgressMessage() throws NoSuchMethodException {
        Method select = VisualAssetMapper.class.getMethod("selectTask", String.class);
        Method update = VisualAssetMapper.class.getMethod("updateTask", com.novelgeneration.novel.vo.AssetTaskVO.class);
        String selectSql = select.getAnnotation(Select.class).value()[0];
        String updateSql = update.getAnnotation(Update.class).value()[0];

        assertTrue(selectSql.contains("message"), "selectTask must return the progress message");
        assertTrue(updateSql.contains("message=#{message}"), "updateTask must persist the progress message");
    }

    private void assertAllParametersNamed(Method method) {
        assertTrue(Arrays.stream(method.getParameters())
                        .allMatch(parameter -> parameter.getAnnotation(Param.class) != null),
                method.getName() + " must declare @Param on every SQL parameter");
    }
}
