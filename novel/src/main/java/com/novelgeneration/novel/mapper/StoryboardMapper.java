package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.vo.StoryboardVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface StoryboardMapper {

    @Select("SELECT id, novel_id AS novelId, chapter_num AS chapterNum, version, status, total_duration_sec AS totalDurationSec, create_time AS createTime, update_time AS updateTime FROM novel_storyboard WHERE novel_id = #{novelId} AND chapter_num = #{chapterNum} ORDER BY version DESC LIMIT 1")
    StoryboardVO selectLatest(@Param("novelId") Long novelId,
                              @Param("chapterNum") Long chapterNum);

    @Select("SELECT id, sequence, duration_sec AS durationSec, location, time_of_day AS timeOfDay, weather, characters, shot_type AS shotType, camera_movement AS cameraMovement, shot_plan AS shotPlan, character_emotion AS characterEmotion, voice_over AS voiceOver, transition, image_prompt AS imagePrompt FROM novel_storyboard_scene WHERE storyboard_id = #{storyboardId} ORDER BY sequence")
    List<StoryboardVO.Scene> selectScenes(Long storyboardId);

    @Select("SELECT asset_id FROM novel_storyboard_asset_ref WHERE storyboard_scene_id=#{sceneId} AND asset_role=#{assetRole} ORDER BY asset_id")
    List<Long> selectAssetIds(@Param("sceneId") Long sceneId,
                              @Param("assetRole") String assetRole);

    @Delete("DELETE r FROM novel_storyboard_asset_ref r "
            + "JOIN novel_storyboard_scene s ON s.id = r.storyboard_scene_id "
            + "JOIN novel_storyboard b ON b.id = s.storyboard_id "
            + "WHERE b.novel_id=#{novelId} AND b.chapter_num=#{chapterNum} "
            + "AND r.storyboard_scene_id=#{sceneId} AND r.asset_id=#{assetId} AND r.asset_role=#{assetRole}")
    int deleteAssetReference(@Param("novelId") Long novelId,
                             @Param("chapterNum") Long chapterNum,
                             @Param("sceneId") Long sceneId,
                             @Param("assetId") Long assetId,
                             @Param("assetRole") String assetRole);

    @Select("SELECT COALESCE(MAX(version), 0) + 1 FROM novel_storyboard WHERE novel_id = #{novelId} AND chapter_num = #{chapterNum}")
    Integer nextVersion(@Param("novelId") Long novelId,
                        @Param("chapterNum") Long chapterNum);

    @Insert("INSERT INTO novel_storyboard (novel_id, chapter_num, version, status, total_duration_sec, create_time, update_time) VALUES (#{novelId}, #{chapterNum}, #{version}, #{status}, #{totalDurationSec}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertStoryboard(StoryboardVO storyboard);

    @Insert("INSERT INTO novel_storyboard_scene (storyboard_id, sequence, duration_sec, location, time_of_day, weather, characters, shot_type, camera_movement, shot_plan, character_emotion, voice_over, transition, image_prompt, create_time, update_time) VALUES (#{storyboardId}, #{scene.sequence}, #{scene.durationSec}, #{scene.location}, #{scene.timeOfDay}, #{scene.weather}, #{scene.characters}, #{scene.shotType}, #{scene.cameraMovement}, #{scene.shotPlan}, #{scene.characterEmotion}, #{scene.voiceOver}, #{scene.transition}, #{scene.imagePrompt}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "scene.id", keyColumn = "id")
    int insertScene(@Param("storyboardId") Long storyboardId,
                    @Param("chapterNum") Long chapterNum,
                    @Param("scene") StoryboardVO.Scene scene);

    @Update("UPDATE novel_storyboard_scene SET sequence=#{sequence}, duration_sec=#{durationSec}, location=#{location}, time_of_day=#{timeOfDay}, weather=#{weather}, characters=#{characters}, shot_type=#{shotType}, camera_movement=#{cameraMovement}, shot_plan=#{shotPlan}, character_emotion=#{characterEmotion}, voice_over=#{voiceOver}, transition=#{transition}, image_prompt=#{imagePrompt}, update_time=NOW() WHERE id=#{id}")
    int updateScene(StoryboardVO.Scene scene);

    @Update("UPDATE novel_storyboard SET total_duration_sec=#{totalDurationSec}, update_time=NOW() WHERE id=#{storyboardId}")
    int updateTotalDuration(@Param("storyboardId") Long storyboardId,
                            @Param("totalDurationSec") Integer totalDurationSec);

    @Delete("DELETE FROM novel_storyboard_scene WHERE storyboard_id IN (SELECT id FROM novel_storyboard WHERE novel_id=#{novelId} AND chapter_num=#{chapterNum})")
    int deleteScenesByNovelAndChapter(@Param("novelId") Long novelId,
                                      @Param("chapterNum") Long chapterNum);

    @Delete("DELETE FROM novel_storyboard WHERE novel_id=#{novelId} AND chapter_num=#{chapterNum}")
    int deleteByNovelAndChapter(@Param("novelId") Long novelId,
                                @Param("chapterNum") Long chapterNum);
}
