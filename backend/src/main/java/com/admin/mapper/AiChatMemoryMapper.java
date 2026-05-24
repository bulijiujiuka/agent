package com.admin.mapper;

import com.admin.entity.AiChatMemory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiChatMemoryMapper {

    @Select("SELECT * FROM ai_chat_memory WHERE conversation_id = #{conversationId} ORDER BY seq ASC")
    List<AiChatMemory> findByConversationId(Long conversationId);

    @Select("SELECT * FROM ai_chat_memory WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<AiChatMemory> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT COALESCE(MAX(seq), 0) FROM ai_chat_memory WHERE conversation_id = #{conversationId}")
    int maxSeq(Long conversationId);

    @Insert("INSERT INTO ai_chat_memory (user_id, conversation_id, role, content, seq) " +
            "VALUES (#{userId}, #{conversationId}, #{role}, #{content}, #{seq})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiChatMemory memory);

    @Delete("DELETE FROM ai_chat_memory WHERE conversation_id = #{conversationId}")
    int deleteByConversationId(Long conversationId);

    @Delete("DELETE FROM ai_chat_memory WHERE conversation_id = #{conversationId} AND seq < #{beforeSeq}")
    int deleteOlderThan(@Param("conversationId") Long conversationId, @Param("beforeSeq") int beforeSeq);
}
