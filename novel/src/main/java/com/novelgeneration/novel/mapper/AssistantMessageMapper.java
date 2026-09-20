package com.novelgeneration.novel.mapper;

import com.novelgeneration.novel.vo.AssistantMessageVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AssistantMessageMapper {

    @Select("SELECT id, message_id AS messageId, conversation_id AS conversationId, sequence_no AS sequenceNo, "
            + "role, content, status, tool_name AS toolName, tool_arguments AS toolArguments, "
            + "tool_result AS toolResult, parent_message_id AS parentMessageId, attempt_no AS attemptNo, "
            + "created_at AS createdAt, persisted_at AS persistedAt FROM assistant_message "
            + "WHERE conversation_id=#{conversationId} ORDER BY sequence_no DESC LIMIT #{limit}")
    List<AssistantMessageVO> selectRecent(@Param("conversationId") String conversationId, @Param("limit") Integer limit);

    @Select("SELECT COALESCE(MAX(sequence_no), 0) + 1 FROM assistant_message WHERE conversation_id=#{conversationId}")
    Long nextSequence(@Param("conversationId") String conversationId);

    @Select("SELECT COUNT(1) FROM assistant_message WHERE conversation_id=#{conversationId}")
    Integer countByConversationId(@Param("conversationId") String conversationId);

    @Insert("INSERT INTO assistant_message "
            + "(message_id, conversation_id, sequence_no, role, content, status, tool_name, tool_arguments, tool_result, parent_message_id, attempt_no, created_at, persisted_at) "
            + "VALUES (#{messageId}, #{conversationId}, #{sequenceNo}, #{role}, #{content}, #{status}, #{toolName}, #{toolArguments}, #{toolResult}, #{parentMessageId}, #{attemptNo}, #{createdAt}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(AssistantMessageVO message);

    @Delete("DELETE FROM assistant_message WHERE conversation_id=#{conversationId}")
    int deleteByConversationId(@Param("conversationId") String conversationId);
}
