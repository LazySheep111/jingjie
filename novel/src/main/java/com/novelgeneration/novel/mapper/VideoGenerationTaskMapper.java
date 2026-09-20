package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.vo.VideoGenerationTaskVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VideoGenerationTaskMapper {
    @Insert("INSERT INTO video_generation_task (novel_id, chapter_num, storyboard_scene_id, first_frame_id, first_frame_path, resolution, status, create_time, update_time) "
            + "VALUES (#{novelId}, #{chapterNum}, #{sceneId}, #{firstFrameId}, #{firstFramePath}, #{resolution}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "taskId", keyColumn = "id")
    int insert(VideoGenerationTaskVO task);

    @Select("SELECT id AS taskId, novel_id AS novelId, chapter_num AS chapterNum, storyboard_scene_id AS sceneId, first_frame_id AS firstFrameId, "
            + "resolution, status, provider_task_id AS providerTaskId, first_frame_path AS firstFramePath, error_message AS errorMessage, "
            + "create_time AS createTime, update_time AS updateTime "
            + "FROM video_generation_task WHERE id=#{taskId}")
    VideoGenerationTaskVO selectById(@Param("taskId") Long taskId);

    @Update("UPDATE video_generation_task SET status=#{status}, provider_task_id=#{providerTaskId}, "
            + "error_message=#{errorMessage}, update_time=NOW() WHERE id=#{taskId}")
    int updateStatus(@Param("taskId") Long taskId,
                     @Param("status") String status,
                     @Param("providerTaskId") String providerTaskId,
                     @Param("errorMessage") String errorMessage);

    @Update("UPDATE video_generation_task SET first_frame_path=#{firstFramePath}, update_time=NOW() WHERE id=#{taskId}")
    int updateFirstFrame(@Param("taskId") Long taskId,
                         @Param("firstFramePath") String firstFramePath);

    @Select("SELECT id AS taskId, novel_id AS novelId, chapter_num AS chapterNum, storyboard_scene_id AS sceneId, first_frame_id AS firstFrameId, "
            + "resolution, status, provider_task_id AS providerTaskId, first_frame_path AS firstFramePath, error_message AS errorMessage, "
            + "create_time AS createTime, update_time AS updateTime "
            + "FROM video_generation_task WHERE novel_id=#{novelId} AND chapter_num=#{chapterNum} AND storyboard_scene_id=#{sceneId} "
            + "AND status IN ('QUEUED','RUNNING','GENERATING_VIDEO') ORDER BY id DESC LIMIT 1")
    VideoGenerationTaskVO selectRunning(@Param("novelId") Long novelId,
                                        @Param("chapterNum") Long chapterNum,
                                        @Param("sceneId") Long sceneId);
}
