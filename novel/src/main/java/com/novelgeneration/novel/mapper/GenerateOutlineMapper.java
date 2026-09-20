package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.entity.NovelInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface GenerateOutlineMapper {

    @Insert("INSERT INTO novel_info (novel_title, category, novel_length, ending_type, writing_style, target_audience, protagonist, role_list, background, world_rule, theme, trigger_event, foreshadow_count, narrative_view, avoid_content) " +
            "VALUES(#{novelTitle}, #{category}, #{novelLength}, #{endingType}, #{writingStyle}, #{targetAudience}, #{protagonist}, #{roleList}, #{background}, #{worldRule}, #{theme}, #{triggerEvent}, #{foreshadowCount}, #{narrativeView}, #{avoidContent})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void add(NovelInfo novelInfo);

    @Select("select coalesce((select novel_title from novel_db.outline where novel_id=#{novelId} limit 1), (select novel_title from novel_db.novel_info where id=#{novelId} limit 1))")
    String selectNovelTitle(Long novelId);

    @Select("select JSON_LENGTH(chapter_list) from novel_db.outline where novel_id=#{novelId}")
    Long selectTotalChapter(Long novelId);
}
