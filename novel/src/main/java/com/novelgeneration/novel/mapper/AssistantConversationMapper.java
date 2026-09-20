package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.vo.AssistantConversationVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AssistantConversationMapper {

    @Select("SELECT id, conversation_id AS conversationId, novel_id AS novelId, title, "
            + "last_message_preview AS lastMessagePreview, message_count AS messageCount, "
            + "conversation_version AS conversationVersion, created_at AS createdAt, updated_at AS updatedAt "
            + "FROM assistant_conversation WHERE novel_id=#{novelId} LIMIT 1")
    AssistantConversationVO selectByNovelId(@Param("novelId") Long novelId);

    @Select("SELECT id, conversation_id AS conversationId, novel_id AS novelId, title, "
            + "last_message_preview AS lastMessagePreview, message_count AS messageCount, "
            + "conversation_version AS conversationVersion, created_at AS createdAt, updated_at AS updatedAt "
            + "FROM assistant_conversation ORDER BY updated_at DESC, id DESC LIMIT #{offset}, #{size}")
    List<AssistantConversationVO> selectPage(@Param("offset") Integer offset, @Param("size") Integer size);

    @Select("SELECT COUNT(1) FROM assistant_conversation")
    Long countAll();

    @Insert("INSERT INTO assistant_conversation "
            + "(conversation_id, novel_id, title, last_message_preview, message_count, conversation_version, created_at, updated_at) "
            + "VALUES (#{conversationId}, #{novelId}, #{title}, NULL, 0, 1, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(AssistantConversationVO conversation);

    @Update("UPDATE assistant_conversation SET title=COALESCE(#{title}, title), last_message_preview=#{lastMessagePreview}, "
            + "message_count=#{messageCount}, updated_at=NOW() WHERE conversation_id=#{conversationId}")
    int updateSummary(AssistantConversationVO conversation);

    @Delete("DELETE FROM assistant_conversation WHERE conversation_id=#{conversationId}")
    int deleteByConversationId(@Param("conversationId") String conversationId);
}
