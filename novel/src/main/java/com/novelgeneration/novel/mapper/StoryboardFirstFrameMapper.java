package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.vo.StoryboardFirstFrameVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface StoryboardFirstFrameMapper {
    String COLUMNS = "id, novel_id AS novelId, chapter_num AS chapterNum, storyboard_scene_id AS sceneId, "
            + "version, status, source AS source, image_path AS imagePath, prompt_snapshot AS promptSnapshot, "
            + "style_snapshot AS styleSnapshot, error_message AS errorMessage, is_deleted AS deleted, "
            + "create_time AS createTime, update_time AS updateTime ";

    @Insert("INSERT INTO storyboard_first_frame (novel_id, chapter_num, storyboard_scene_id, version, status, source, "
            + "image_path, prompt_snapshot, style_snapshot, error_message, is_deleted, create_time, update_time) "
            + "VALUES (#{novelId}, #{chapterNum}, #{sceneId}, #{version}, #{status}, #{source}, #{imagePath}, "
            + "#{promptSnapshot}, #{styleSnapshot}, #{errorMessage}, 0, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(StoryboardFirstFrameVO frame);

    @Select("SELECT " + COLUMNS + "FROM storyboard_first_frame WHERE id=#{id}")
    StoryboardFirstFrameVO selectById(@Param("id") Long id);

    @Select("SELECT " + COLUMNS + "FROM storyboard_first_frame WHERE id=#{id} AND is_deleted=0 AND status='SUCCESS'")
    StoryboardFirstFrameVO selectAvailableById(@Param("id") Long id);

    @Select("SELECT " + COLUMNS + "FROM storyboard_first_frame WHERE novel_id=#{novelId} AND chapter_num=#{chapterNum} "
            + "AND storyboard_scene_id=#{sceneId} AND is_deleted=0 ORDER BY version DESC")
    List<StoryboardFirstFrameVO> selectByScene(@Param("novelId") Long novelId,
                                                @Param("chapterNum") Long chapterNum,
                                                @Param("sceneId") Long sceneId);

    @Select("SELECT " + COLUMNS + "FROM storyboard_first_frame WHERE novel_id=#{novelId} AND chapter_num=#{chapterNum} "
            + "AND storyboard_scene_id=#{sceneId} AND is_deleted=0 AND status IN ('QUEUED','GENERATING') "
            + "ORDER BY id DESC LIMIT 1")
    StoryboardFirstFrameVO selectRunning(@Param("novelId") Long novelId,
                                          @Param("chapterNum") Long chapterNum,
                                          @Param("sceneId") Long sceneId);

    @Select("SELECT COALESCE(MAX(version), 0) + 1 FROM storyboard_first_frame WHERE storyboard_scene_id=#{sceneId}")
    Integer nextVersion(@Param("sceneId") Long sceneId);

    @Update("UPDATE storyboard_first_frame SET status=#{status}, image_path=#{imagePath}, error_message=#{errorMessage}, "
            + "update_time=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("imagePath") String imagePath,
                     @Param("errorMessage") String errorMessage);

    @Update("UPDATE storyboard_first_frame SET is_deleted=1, update_time=NOW() WHERE id=#{id}")
    int softDelete(@Param("id") Long id);
}
