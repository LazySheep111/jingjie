package com.novelgeneration.novel.mapper;


import com.novelgeneration.novel.dto.OutlineDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SaveOutlineMapper {

    @Update("UPDATE outline SET novel_title = #{formData.novelTitle}, category = #{formData.category}, protagonist = #{formData.protagonist}, role_list = #{roleListJson}, outline_title = #{outline.novelTitle}, overall_plot = #{outline.overallPlot}, foreshadow_list = #{foreshadowListJson}, chapter_list = #{chapterListJson}, start_full_generation = #{startFullGeneration}, create_time = #{createTime}, update_time = #{updateTime} WHERE novel_id = #{novelId}")
    int updateOutline(OutlineDTO outlineDTO);

    @Insert("INSERT INTO outline (novel_id, novel_title, category, protagonist, role_list, outline_title, overall_plot, foreshadow_list, chapter_list, start_full_generation, create_time, update_time) " +
            "VALUES (#{novelId}, #{formData.novelTitle}, #{formData.category}, #{formData.protagonist}, #{roleListJson}, #{outline.novelTitle}, #{outline.overallPlot}, #{foreshadowListJson}, #{chapterListJson}, #{startFullGeneration}, #{createTime}, #{updateTime})")
    int insertOutline(OutlineDTO outlineDTO);
}
