package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.vo.ChapterVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChapterMapper {

    @Select("select novel_id as novelId, chapter_num as chapterNum, chapter_title as chapterTitle, chapter_summary as chapterSummary, chapter_text as chapterText from novel_db.novel_chapter_text where novel_id=#{novelId} order by chapter_num")
    List<ChapterVO> selectChapter(Long novelId);

    @Select("select novel_id as novelId, chapter_num as chapterNum, chapter_title as chapterTitle, chapter_summary as chapterSummary, chapter_text as chapterText from novel_db.novel_chapter_text where novel_id=#{novelId} and chapter_num=#{chapterNum}")
    ChapterVO selectChapterByNum(@org.apache.ibatis.annotations.Param("novelId") Long novelId,
                                 @org.apache.ibatis.annotations.Param("chapterNum") Long chapterNum);
}
