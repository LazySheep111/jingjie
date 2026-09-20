package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.vo.StoryboardVideoVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface StoryboardVideoMapper {
    String COLUMNS = "id, novel_id AS novelId, chapter_num AS chapterNum, storyboard_scene_id AS sceneId, version, "
            + "source AS source, video_path AS videoPath, first_frame_path AS firstFramePath, duration_sec AS durationSec, resolution AS resolution, "
            + "prompt_snapshot AS promptSnapshot, style_snapshot AS styleSnapshot, is_current AS current, "
            + "is_deleted AS deleted, create_time AS createTime ";

    @Select("SELECT " + COLUMNS + "FROM storyboard_video WHERE novel_id=#{novelId} AND chapter_num=#{chapterNum} "
            + "AND storyboard_scene_id=#{sceneId} AND is_deleted=0 "
            + "ORDER BY version DESC")
    List<StoryboardVideoVO> selectByScene(@Param("novelId") Long novelId,
                                          @Param("chapterNum") Long chapterNum,
                                          @Param("sceneId") Long sceneId);

    @Select("SELECT COALESCE(MAX(version), 0) + 1 FROM storyboard_video WHERE storyboard_scene_id=#{sceneId}")
    Integer nextVersion(@Param("sceneId") Long sceneId);

    @Update("UPDATE storyboard_video SET is_current=0 WHERE storyboard_scene_id=#{sceneId}")
    int clearCurrent(@Param("sceneId") Long sceneId);

    @Insert("INSERT INTO storyboard_video (novel_id, chapter_num, storyboard_scene_id, version, source, video_path, "
            + "first_frame_path, duration_sec, resolution, prompt_snapshot, style_snapshot, is_current, is_deleted, create_time) "
            + "VALUES (#{novelId}, #{chapterNum}, #{sceneId}, #{version}, #{source}, #{videoPath}, #{firstFramePath}, "
            + "#{durationSec}, #{resolution}, #{promptSnapshot}, #{styleSnapshot}, 1, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(StoryboardVideoVO video);

    @Insert("INSERT INTO storyboard_video_asset (video_id, asset_id, asset_version, asset_name, asset_type, image_path) "
            + "VALUES (#{videoId}, #{assetId}, #{assetVersion}, #{assetName}, #{assetType}, #{imagePath})")
    int insertAsset(StoryboardVideoVO.AssetSnapshot asset);

    @Update("UPDATE storyboard_video SET is_current=1 WHERE id=#{videoId}")
    int setCurrent(@Param("videoId") Long videoId);

    @Select("SELECT storyboard_scene_id FROM storyboard_video WHERE id=#{videoId} AND is_deleted=0")
    Long selectSceneId(@Param("videoId") Long videoId);

    @Select("SELECT " + COLUMNS + "FROM storyboard_video WHERE id=#{videoId}")
    StoryboardVideoVO selectById(@Param("videoId") Long videoId);

    @Update("UPDATE storyboard_video SET is_deleted=1, is_current=0 WHERE id=#{videoId}")
    int softDelete(@Param("videoId") Long videoId);

    @Select("SELECT " + COLUMNS + "FROM storyboard_video WHERE storyboard_scene_id=#{sceneId} "
            + "AND is_deleted=0 ORDER BY version DESC LIMIT 1")
    StoryboardVideoVO selectLatestAvailableByScene(@Param("sceneId") Long sceneId);

    @Select("SELECT video_id AS videoId, asset_id AS assetId, asset_version AS assetVersion, asset_name AS assetName, asset_type AS assetType, image_path AS imagePath "
            + "FROM storyboard_video_asset WHERE video_id=#{videoId} ORDER BY asset_id")
    List<StoryboardVideoVO.AssetSnapshot> selectAssets(@Param("videoId") Long videoId);
}
