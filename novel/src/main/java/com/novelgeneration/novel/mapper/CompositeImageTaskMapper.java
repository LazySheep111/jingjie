package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.vo.CompositeImageTaskVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CompositeImageTaskMapper {

    @Select("SELECT id AS taskId, novel_id AS novelId, asset_id AS assetId, asset_version AS assetVersion, "
            + "status, image_path AS imagePath, error_message AS errorMessage, create_time AS createTime, update_time AS updateTime "
            + "FROM novel_composite_image_task WHERE novel_id=#{novelId} AND asset_id=#{assetId} "
            + "AND asset_version=#{assetVersion} AND status IN ('PENDING', 'PROCESSING') "
            + "ORDER BY create_time DESC LIMIT 1")
    CompositeImageTaskVO selectRunning(@Param("novelId") Long novelId,
                                       @Param("assetId") Long assetId,
                                       @Param("assetVersion") Integer assetVersion);

    @Select("SELECT id AS taskId, novel_id AS novelId, asset_id AS assetId, asset_version AS assetVersion, "
            + "status, image_path AS imagePath, error_message AS errorMessage, create_time AS createTime, update_time AS updateTime "
            + "FROM novel_composite_image_task WHERE id=#{taskId}")
    CompositeImageTaskVO selectById(@Param("taskId") String taskId);

    @Insert("INSERT INTO novel_composite_image_task "
            + "(id, novel_id, asset_id, asset_version, status, create_time, update_time) "
            + "VALUES (#{taskId}, #{novelId}, #{assetId}, #{assetVersion}, #{status}, NOW(), NOW())")
    int insert(CompositeImageTaskVO task);

    @Update("UPDATE novel_composite_image_task SET status='PROCESSING', update_time=NOW() "
            + "WHERE id=#{taskId} AND status='PENDING'")
    int claim(@Param("taskId") String taskId);

    @Update("UPDATE novel_composite_image_task SET status='COMPLETED', image_path=#{imagePath}, "
            + "error_message=NULL, update_time=NOW() WHERE id=#{taskId}")
    int complete(@Param("taskId") String taskId, @Param("imagePath") String imagePath);

    @Update("UPDATE novel_composite_image_task SET status='FAILED', error_message=#{errorMessage}, update_time=NOW() "
            + "WHERE id=#{taskId}")
    int fail(@Param("taskId") String taskId, @Param("errorMessage") String errorMessage);
}
