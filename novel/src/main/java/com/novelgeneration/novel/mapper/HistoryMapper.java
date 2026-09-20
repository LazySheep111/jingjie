package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.entity.NovelInfo;
import com.novelgeneration.novel.vo.HistoryOutlineRecordVO;
import com.novelgeneration.novel.vo.NovelHistoryItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface HistoryMapper {

    @Select("SELECT ni.id AS novelId, " +
            "COALESCE(o.outline_title, ni.novel_title) AS novelTitle, " +
            "COALESCE(JSON_LENGTH(o.chapter_list), (SELECT COUNT(1) FROM novel_chapter_text nct WHERE nct.novel_id = ni.id), 0) AS totalChapter, " +
            "o.overall_plot AS overallPlot, " +
            "CASE WHEN EXISTS (SELECT 1 FROM novel_chapter_text nct WHERE nct.novel_id = ni.id) THEN 'FULL_GENERATED' " +
            "WHEN o.novel_id IS NOT NULL THEN 'OUTLINE_ONLY' ELSE 'OUTLINE_ONLY' END AS status, " +
            "ni.create_time AS createTime " +
            "FROM novel_info ni " +
            "LEFT JOIN outline o ON o.novel_id = ni.id " +
            "WHERE IFNULL(ni.is_deleted, 0) = 0 " +
            "ORDER BY ni.create_time DESC, ni.id DESC " +
            "LIMIT #{offset}, #{pageSize}")
    List<NovelHistoryItemVO> selectHistoryList(@Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    @Select("SELECT COUNT(1) FROM novel_info WHERE IFNULL(is_deleted, 0) = 0")
    Long countHistory();

    @Select("SELECT id, novel_title AS novelTitle, category, novel_length AS novelLength, ending_type AS endingType, " +
            "writing_style AS writingStyle, target_audience AS targetAudience, protagonist, role_list AS roleList, " +
            "background, world_rule AS worldRule, theme, trigger_event AS triggerEvent, foreshadow_count AS foreshadowCount, " +
            "narrative_view AS narrativeView, avoid_content AS avoidContent, is_deleted AS isDeleted, create_time AS createTime, update_time AS updateTime " +
            "FROM novel_info WHERE id = #{novelId} AND IFNULL(is_deleted, 0) = 0")
    NovelInfo selectNovelInfo(Long novelId);

    @Select("SELECT novel_id AS novelId, outline_title AS novelTitle, overall_plot AS overallPlot, " +
            "foreshadow_list AS foreshadowListJson, chapter_list AS chapterListJson, start_full_generation AS startFullGeneration " +
            "FROM outline WHERE novel_id = #{novelId}")
    HistoryOutlineRecordVO selectOutlineRecord(Long novelId);

    @Select("SELECT COUNT(1) FROM novel_chapter_text WHERE novel_id = #{novelId}")
    Integer countChapters(Long novelId);

    @Update("UPDATE novel_info SET is_deleted = 1, update_time = NOW() WHERE id = #{novelId}")
    int softDeleteNovel(Long novelId);
}
