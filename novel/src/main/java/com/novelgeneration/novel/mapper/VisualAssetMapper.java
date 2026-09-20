package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.vo.AssetTaskVO;
import com.novelgeneration.novel.vo.VisualAssetVO;
import com.novelgeneration.novel.vo.NovelLibraryItemVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface VisualAssetMapper {

    @Select("SELECT ni.id AS novelId, ni.novel_title AS novelTitle, ni.category, "
            + "COUNT(DISTINCT CASE WHEN a.status <> 'MERGED' THEN a.id END) AS assetCount, "
            + "COUNT(DISTINCT CASE WHEN a.status <> 'MERGED' AND a.asset_type = 'CHARACTER' THEN a.id END) AS characterCount, "
            + "COUNT(DISTINCT CASE WHEN a.status <> 'MERGED' AND a.asset_type = 'LOCATION' THEN a.id END) AS locationCount "
            + "FROM novel_info ni LEFT JOIN novel_visual_asset a ON a.novel_id = ni.id "
            + "WHERE IFNULL(ni.is_deleted, 0) = 0 GROUP BY ni.id, ni.novel_title, ni.category "
            + "ORDER BY ni.create_time DESC, ni.id DESC")
    List<NovelLibraryItemVO> selectNovelsForLibrary();

    @Select("SELECT a.id AS assetId, v.id AS versionId, a.novel_id AS novelId, ni.novel_title AS novelTitle, "
            + "a.asset_type AS assetType, a.normalized_name AS normalizedName, a.display_name AS assetName, "
            + "v.version, v.core_features AS coreFeatures, v.front_prompt AS frontPrompt, "
            + "v.side_prompt AS sidePrompt, v.back_prompt AS backPrompt, v.composite_image_path AS compositeImagePath, v.status, "
            + "(SELECT COUNT(*) FROM novel_storyboard_asset_ref r WHERE r.asset_id = a.id) AS reuseCount, "
            + "(SELECT GROUP_CONCAT(DISTINCT b.chapter_num ORDER BY b.chapter_num SEPARATOR ',') "
            + " FROM novel_storyboard_asset_ref r2 JOIN novel_storyboard_scene s2 ON s2.id = r2.storyboard_scene_id "
            + " JOIN novel_storyboard b ON b.id = s2.storyboard_id WHERE r2.asset_id = a.id) AS chapterNumbers, "
            + "v.create_time AS createTime, v.update_time AS updateTime "
            + "FROM novel_visual_asset a JOIN novel_visual_asset_version v ON v.asset_id = a.id AND v.version = a.current_version "
            + "JOIN novel_info ni ON ni.id = a.novel_id "
            + "WHERE a.status <> 'MERGED' AND IFNULL(ni.is_deleted, 0) = 0 "
            + "AND (#{novelId} IS NULL OR a.novel_id = #{novelId}) "
            + "AND (#{assetType} IS NULL OR a.asset_type = #{assetType}) "
            + "AND (#{keyword} IS NULL OR a.display_name LIKE CONCAT('%', #{keyword}, '%')) "
            + "ORDER BY a.update_time DESC, a.id DESC LIMIT #{offset}, #{pageSize}")
    List<VisualAssetVO> selectLibrary(@Param("novelId") Long novelId,
                                      @Param("assetType") String assetType,
                                      @Param("keyword") String keyword,
                                      @Param("offset") Integer offset,
                                      @Param("pageSize") Integer pageSize);

    @Select("SELECT COUNT(*) FROM novel_visual_asset a JOIN novel_info ni ON ni.id = a.novel_id "
            + "WHERE a.status <> 'MERGED' AND IFNULL(ni.is_deleted, 0) = 0 "
            + "AND (#{novelId} IS NULL OR a.novel_id = #{novelId}) "
            + "AND (#{assetType} IS NULL OR a.asset_type = #{assetType}) "
            + "AND (#{keyword} IS NULL OR a.display_name LIKE CONCAT('%', #{keyword}, '%'))")
    Long countLibrary(@Param("novelId") Long novelId,
                      @Param("assetType") String assetType,
                      @Param("keyword") String keyword);

    @Select("SELECT a.id AS assetId, v.id AS versionId, a.novel_id AS novelId, ni.novel_title AS novelTitle, "
            + "a.asset_type AS assetType, a.normalized_name AS normalizedName, a.display_name AS assetName, "
            + "v.version, v.core_features AS coreFeatures, v.front_prompt AS frontPrompt, v.side_prompt AS sidePrompt, "
            + "v.back_prompt AS backPrompt, v.composite_image_path AS compositeImagePath, v.status, "
            + "(SELECT COUNT(*) FROM novel_storyboard_asset_ref r WHERE r.asset_id = a.id) AS reuseCount, "
            + "(SELECT GROUP_CONCAT(DISTINCT b.chapter_num ORDER BY b.chapter_num SEPARATOR ',') "
            + " FROM novel_storyboard_asset_ref r2 JOIN novel_storyboard_scene s2 ON s2.id = r2.storyboard_scene_id "
            + " JOIN novel_storyboard b ON b.id = s2.storyboard_id WHERE r2.asset_id = a.id) AS chapterNumbers, "
            + "v.create_time AS createTime, v.update_time AS updateTime "
            + "FROM novel_visual_asset a JOIN novel_visual_asset_version v ON v.asset_id = a.id AND v.version = a.current_version "
            + "JOIN novel_info ni ON ni.id = a.novel_id WHERE a.id = #{assetId} AND a.status <> 'MERGED' "
            + "AND IFNULL(ni.is_deleted, 0) = 0")
    VisualAssetVO selectGlobalAsset(@Param("assetId") Long assetId);

    @Select("SELECT chapter_num FROM novel_chapter_text WHERE novel_id = #{novelId} ORDER BY chapter_num")
    List<Long> selectChapterNumbers(@Param("novelId") Long novelId);

    @Select("SELECT s.id FROM novel_storyboard_scene s JOIN novel_storyboard b ON b.id = s.storyboard_id "
            + "WHERE b.novel_id = #{novelId} AND b.chapter_num = #{chapterNum} ORDER BY s.sequence")
    List<Long> selectSceneIds(@Param("novelId") Long novelId, @Param("chapterNum") Long chapterNum);

    @Select("SELECT a.id AS assetId, v.id AS versionId, a.novel_id AS novelId, "
            + "a.asset_type AS assetType, a.normalized_name AS normalizedName, "
            + "a.display_name AS assetName, v.version, v.core_features AS coreFeatures, "
            + "v.front_prompt AS frontPrompt, v.side_prompt AS sidePrompt, "
            + "v.back_prompt AS backPrompt, v.composite_image_path AS compositeImagePath, v.status, "
            + "(SELECT COUNT(*) FROM novel_storyboard_asset_ref r WHERE r.asset_id = a.id) AS reuseCount, "
            + "v.create_time AS createTime, v.update_time AS updateTime "
            + "FROM novel_visual_asset a JOIN novel_visual_asset_version v "
            + "ON v.asset_id = a.id AND v.version = a.current_version "
            + "WHERE a.novel_id = #{novelId} AND a.asset_type = #{assetType} "
            + "AND a.normalized_name = #{normalizedName} AND a.status <> 'MERGED'")
    VisualAssetVO selectAsset(@Param("novelId") Long novelId,
                              @Param("assetType") String assetType,
                              @Param("normalizedName") String normalizedName);

    @Select("SELECT a.id AS assetId, v.id AS versionId, a.novel_id AS novelId, "
            + "a.asset_type AS assetType, a.normalized_name AS normalizedName, "
            + "a.display_name AS assetName, v.version, v.core_features AS coreFeatures, "
            + "v.front_prompt AS frontPrompt, v.side_prompt AS sidePrompt, "
            + "v.back_prompt AS backPrompt, v.composite_image_path AS compositeImagePath, v.status, "
            + "(SELECT COUNT(*) FROM novel_storyboard_asset_ref r WHERE r.asset_id = a.id) AS reuseCount, "
            + "v.create_time AS createTime, v.update_time AS updateTime "
            + "FROM novel_visual_asset a JOIN novel_visual_asset_version v "
            + "ON v.asset_id = a.id AND v.version = a.current_version "
            + "WHERE a.novel_id = #{novelId} AND a.status <> 'MERGED' "
            + "AND (#{assetType} IS NULL OR a.asset_type = #{assetType}) ORDER BY a.display_name")
    List<VisualAssetVO> selectAssets(@Param("novelId") Long novelId,
                                     @Param("assetType") String assetType);

    @Select("SELECT a.id AS assetId, v.id AS versionId, a.novel_id AS novelId, "
            + "a.asset_type AS assetType, a.normalized_name AS normalizedName, "
            + "a.display_name AS assetName, v.version, v.core_features AS coreFeatures, "
            + "v.front_prompt AS frontPrompt, v.side_prompt AS sidePrompt, "
            + "v.back_prompt AS backPrompt, v.composite_image_path AS compositeImagePath, v.status, "
            + "(SELECT COUNT(*) FROM novel_storyboard_asset_ref r WHERE r.asset_id = a.id) AS reuseCount, "
            + "v.create_time AS createTime, v.update_time AS updateTime "
            + "FROM novel_visual_asset a JOIN novel_visual_asset_version v ON v.asset_id = a.id "
            + "WHERE a.id = #{assetId} AND ((#{version} IS NULL AND v.version = a.current_version) OR v.version = #{version})")
    VisualAssetVO selectAssetVersion(@Param("assetId") Long assetId,
                                     @Param("version") Integer version);

    @Insert("INSERT INTO novel_visual_asset "
            + "(novel_id, asset_type, normalized_name, display_name, current_version, status, create_time, update_time) "
            + "VALUES (#{novelId}, #{assetType}, #{normalizedName}, #{assetName}, #{version}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "assetId", keyColumn = "id")
    int insertAsset(VisualAssetVO asset);

    @Insert("INSERT INTO novel_visual_asset_version "
            + "(asset_id, version, core_features, front_prompt, side_prompt, back_prompt, composite_image_path, status, create_time, update_time) "
            + "VALUES (#{assetId}, #{version}, #{coreFeatures}, #{frontPrompt}, #{sidePrompt}, #{backPrompt}, #{compositeImagePath}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "versionId", keyColumn = "id")
    int insertAssetVersion(VisualAssetVO asset);

    @Update("UPDATE novel_visual_asset SET display_name=#{assetName}, normalized_name=#{normalizedName}, "
            + "current_version=#{version}, status=#{status}, update_time=NOW() WHERE id=#{assetId} AND novel_id=#{novelId}")
    int updateAsset(VisualAssetVO asset);

    @Update("UPDATE novel_visual_asset_version SET core_features=#{coreFeatures}, front_prompt=#{frontPrompt}, "
            + "side_prompt=#{sidePrompt}, back_prompt=#{backPrompt}, composite_image_path=#{compositeImagePath}, status=#{status}, update_time=NOW() "
            + "WHERE id=#{versionId} AND asset_id=#{assetId} AND version=#{version}")
    int updateAssetVersion(VisualAssetVO asset);

    @Update("UPDATE novel_visual_asset_version SET composite_image_path=#{imagePath}, status='IMAGE_READY', update_time=NOW() "
            + "WHERE asset_id=#{assetId} AND version=#{version}")
    int updateCompositeImagePath(@Param("assetId") Long assetId,
                                 @Param("version") Integer version,
                                 @Param("imagePath") String imagePath);

    @Update("UPDATE novel_storyboard_asset_ref SET asset_version=#{version} WHERE asset_id=#{assetId}")
    int updateAssetReferenceVersions(@Param("assetId") Long assetId,
                                     @Param("version") Integer version);

    @Select("SELECT DISTINCT a.id AS assetId, a.novel_id AS novelId, a.asset_type AS assetType, "
            + "a.display_name AS assetName, a.current_version AS version, "
            + "v.core_features AS coreFeatures, v.front_prompt AS frontPrompt, "
            + "v.side_prompt AS sidePrompt, v.back_prompt AS backPrompt, v.composite_image_path AS compositeImagePath, v.status "
            + "FROM novel_visual_asset a JOIN novel_visual_asset_version v "
            + "ON v.asset_id = a.id AND v.version = a.current_version "
            + "JOIN novel_storyboard_asset_ref r ON r.asset_id = a.id "
            + "JOIN novel_storyboard_scene s ON s.id = r.storyboard_scene_id "
            + "JOIN novel_storyboard b ON b.id = s.storyboard_id "
            + "WHERE b.novel_id = #{novelId} AND b.chapter_num = #{chapterNum} "
            + "AND a.status <> 'MERGED' ORDER BY a.display_name")
    List<VisualAssetVO> selectAssetsByChapter(@Param("novelId") Long novelId,
                                              @Param("chapterNum") Long chapterNum);

    @Select("SELECT COUNT(*) FROM novel_storyboard_asset_ref WHERE asset_id=#{assetId}")
    int countAssetReferences(@Param("assetId") Long assetId);

    @Insert("INSERT IGNORE INTO novel_storyboard_asset_ref "
            + "(storyboard_scene_id, asset_id, asset_version, asset_role, create_time) "
            + "VALUES (#{sceneId}, #{assetId}, #{version}, #{assetRole}, NOW())")
    int insertSceneAssetRef(@Param("sceneId") Long sceneId,
                            @Param("assetId") Long assetId,
                            @Param("version") Integer version,
                            @Param("assetRole") String assetRole);

    @Delete("DELETE FROM novel_storyboard_asset_ref WHERE asset_id=#{sourceAssetId} "
            + "AND asset_role=#{assetRole} AND storyboard_scene_id IN "
            + "(SELECT s.id FROM novel_storyboard_scene s JOIN novel_storyboard b ON b.id=s.storyboard_id "
            + "WHERE b.novel_id=#{novelId} AND b.chapter_num=#{chapterNum})")
    int deleteChapterAssetReferences(@Param("novelId") Long novelId,
                                     @Param("chapterNum") Long chapterNum,
                                     @Param("sourceAssetId") Long sourceAssetId,
                                     @Param("assetRole") String assetRole);

    @Update("UPDATE novel_storyboard_asset_ref SET asset_id=#{sourceAssetId}, asset_version=#{sourceVersion} "
            + "WHERE asset_id=#{targetAssetId} AND asset_role=#{assetRole} AND storyboard_scene_id IN "
            + "(SELECT s.id FROM novel_storyboard_scene s JOIN novel_storyboard b ON b.id=s.storyboard_id "
            + "WHERE b.novel_id=#{novelId} AND b.chapter_num=#{chapterNum})")
    int replaceChapterAssetReferences(@Param("novelId") Long novelId,
                                      @Param("chapterNum") Long chapterNum,
                                      @Param("targetAssetId") Long targetAssetId,
                                      @Param("sourceAssetId") Long sourceAssetId,
                                      @Param("sourceVersion") Integer sourceVersion,
                                      @Param("assetRole") String assetRole);

    @Delete("DELETE FROM novel_storyboard_asset_ref WHERE storyboard_scene_id=#{sceneId}")
    int deleteSceneAssetRefs(@Param("sceneId") Long sceneId);

    @Update("UPDATE novel_storyboard_asset_ref SET asset_id=#{targetAssetId}, asset_version=#{targetVersion} "
            + "WHERE asset_id=#{sourceAssetId}")
    int replaceAssetReferences(@Param("sourceAssetId") Long sourceAssetId,
                               @Param("targetAssetId") Long targetAssetId,
                               @Param("targetVersion") Integer targetVersion);

    @Update("UPDATE novel_visual_asset SET status='MERGED', update_time=NOW() "
            + "WHERE id=#{assetId} AND novel_id=#{novelId}")
    int markMerged(@Param("novelId") Long novelId, @Param("assetId") Long assetId);

    @Select("SELECT id AS taskId, novel_id AS novelId, chapter_num AS chapterNum, status, total, reused, created, failed, error_message AS errorMessage, message, create_time AS createTime, update_time AS updateTime "
            + "FROM novel_asset_extract_task WHERE novel_id=#{novelId} AND chapter_num=#{chapterNum} "
            + "AND status IN ('PENDING', 'PROCESSING') ORDER BY create_time DESC LIMIT 1")
    AssetTaskVO selectRunningTask(@Param("novelId") Long novelId,
                                  @Param("chapterNum") Long chapterNum);

    @Select("SELECT id AS taskId, novel_id AS novelId, chapter_num AS chapterNum, status, total, reused, created, failed, error_message AS errorMessage, message, create_time AS createTime, update_time AS updateTime "
            + "FROM novel_asset_extract_task WHERE id=#{taskId}")
    AssetTaskVO selectTask(@Param("taskId") String taskId);

    @Insert("INSERT INTO novel_asset_extract_task (id, novel_id, chapter_num, status, total, reused, created, failed, error_message, create_time, update_time) "
            + "VALUES (#{taskId}, #{novelId}, #{chapterNum}, #{status}, #{total}, #{reused}, #{created}, #{failed}, #{errorMessage}, NOW(), NOW())")
    int insertTask(AssetTaskVO task);

    @Update("UPDATE novel_asset_extract_task SET status='PROCESSING', update_time=NOW() "
            + "WHERE id=#{taskId} AND status='PENDING'")
    int claimTask(@Param("taskId") String taskId);

    @Update("UPDATE novel_asset_extract_task SET status='PENDING', message='任务超时，重新排队', update_time=NOW() "
            + "WHERE id=#{taskId} AND status='PROCESSING' "
            + "AND TIMESTAMPDIFF(SECOND, update_time, NOW()) >= #{timeoutSeconds}")
    int resetStaleTask(@Param("taskId") String taskId,
                       @Param("timeoutSeconds") int timeoutSeconds);

    @Update("UPDATE novel_asset_extract_task SET status=#{status}, total=#{total}, reused=#{reused}, created=#{created}, failed=#{failed}, error_message=#{errorMessage}, message=#{message}, update_time=NOW() WHERE id=#{taskId}")
    int updateTask(AssetTaskVO task);
}
