package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.vo.NovelVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NovelTextMapper {

    @Insert("insert into novel_chapter_text (novel_id,chapter_num,chapter_title,chapter_summary,chapter_text,create_time,update_time)" +
            "VALUES (#{novelId},#{chapter.chapterNum},#{chapter.chapterTitle},#{chapter.chapterSummary},#{chapter.chapterText},#{createTime},#{updateTime})")
    void insertChapter(@Param("novelId") Long novelId,
                       @Param("chapter") NovelVO.NovelText chapter,
                       @Param("createTime") LocalDateTime createTime,
                       @Param("updateTime") LocalDateTime updateTime);

    @Select("select chapter_num as chapterNum, chapter_title as chapterTitle, chapter_summary as chapterSummary, chapter_text as chapterText from novel_db.novel_chapter_text where novel_id=#{novelId} order by chapter_num")
    List<NovelVO.NovelText> selectNovelText(Long novelId);

    @Delete("delete from novel_chapter_text where novel_id=#{novelId}")
    void deleteByNovelId(Long novelId);
}
