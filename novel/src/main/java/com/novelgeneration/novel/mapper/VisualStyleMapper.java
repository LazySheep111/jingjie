package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.vo.VisualStyleVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VisualStyleMapper {
    @Select("SELECT id, novel_id AS novelId, era, region, architecture, material, "
            + "color_style AS colorStyle, lighting_style AS lightingStyle, art_style AS artStyle, "
            + "camera_style AS cameraStyle, positive_prompt AS positivePrompt, negative_prompt AS negativePrompt, "
            + "version, status, create_time AS createTime, update_time AS updateTime "
            + "FROM novel_visual_style WHERE novel_id=#{novelId}")
    VisualStyleVO selectByNovelId(@Param("novelId") Long novelId);

    @Insert("INSERT INTO novel_visual_style "
            + "(novel_id, era, region, architecture, material, color_style, lighting_style, art_style, "
            + "camera_style, positive_prompt, negative_prompt, version, status, create_time, update_time) "
            + "VALUES (#{novelId}, #{era}, #{region}, #{architecture}, #{material}, #{colorStyle}, "
            + "#{lightingStyle}, #{artStyle}, #{cameraStyle}, #{positivePrompt}, #{negativePrompt}, 1, 'ACTIVE', NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(VisualStyleVO style);

    @Update("UPDATE novel_visual_style SET era=#{era}, region=#{region}, architecture=#{architecture}, "
            + "material=#{material}, color_style=#{colorStyle}, lighting_style=#{lightingStyle}, "
            + "art_style=#{artStyle}, camera_style=#{cameraStyle}, positive_prompt=#{positivePrompt}, "
            + "negative_prompt=#{negativePrompt}, version=version+1, status='ACTIVE', update_time=NOW() "
            + "WHERE novel_id=#{novelId}")
    int update(VisualStyleVO style);
}
