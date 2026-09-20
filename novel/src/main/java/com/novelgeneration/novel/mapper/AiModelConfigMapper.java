package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.entity.AiModelConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AiModelConfigMapper {
    @Select("SELECT id, capability_type AS capabilityType, provider_type AS providerType, "
            + "api_url AS apiUrl, query_url AS queryUrl, encrypted_api_key AS encryptedApiKey, "
            + "model_name AS modelName, enabled, test_status AS testStatus, last_test_at AS lastTestAt, "
            + "last_error AS lastError, version, create_time AS createTime, update_time AS updateTime "
            + "FROM ai_model_config WHERE capability_type=#{capabilityType} AND enabled=TRUE "
            + "ORDER BY version DESC, id DESC LIMIT 1")
    AiModelConfig selectActive(@Param("capabilityType") String capabilityType);

    @Select("SELECT COALESCE(MAX(version), 0) FROM ai_model_config WHERE capability_type=#{capabilityType}")
    Integer selectLatestVersion(@Param("capabilityType") String capabilityType);

    @Insert("INSERT INTO ai_model_config (capability_type, provider_type, api_url, query_url, "
            + "encrypted_api_key, model_name, enabled, test_status, last_test_at, last_error, version, create_time, update_time) "
            + "VALUES (#{capabilityType}, #{providerType}, #{apiUrl}, #{queryUrl}, #{encryptedApiKey}, #{modelName}, "
            + "#{enabled}, #{testStatus}, #{lastTestAt}, #{lastError}, #{version}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertVersion(AiModelConfig config);

    @Update("UPDATE ai_model_config SET enabled=FALSE, update_time=NOW() "
            + "WHERE capability_type=#{capabilityType} AND enabled=TRUE")
    int disableActive(@Param("capabilityType") String capabilityType);
}
