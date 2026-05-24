package com.admin.mapper;

import com.admin.entity.AiMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiMessageMapper {

    @Select("SELECT * FROM ai_message WHERE conversation_id = #{conversationId} ORDER BY create_time ASC")
    List<AiMessage> findByConversationId(Long conversationId);

    @Select("SELECT * FROM ai_message WHERE id = #{id}")
    AiMessage findById(Long id);

    @Insert("INSERT INTO ai_message (conversation_id, role, content, reference_content, token_usage, feedback, response_ms, retrieval_score, retrieval_hit_count) " +
            "VALUES (#{conversationId}, #{role}, #{content}, #{referenceContent}, #{tokenUsage}, #{feedback}, #{responseMs}, #{retrievalScore}, #{retrievalHitCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiMessage message);

    @Update("UPDATE ai_message SET feedback = #{feedback} WHERE id = #{id}")
    int updateFeedback(@Param("id") Long id, @Param("feedback") String feedback);

    @Select("SELECT COUNT(*) FROM ai_message WHERE feedback = #{feedback}")
    long countByFeedback(@Param("feedback") String feedback);

    @Select("SELECT COUNT(*) FROM ai_message WHERE role = 'ASSISTANT'")
    long countAssistantMessages();

    @Select("SELECT COALESCE(AVG(response_ms), 0) FROM ai_message WHERE role = 'ASSISTANT' AND response_ms IS NOT NULL")
    long avgResponseMs();

    @Select("SELECT COALESCE(SUM(token_usage), 0) FROM ai_message WHERE token_usage IS NOT NULL")
    long totalTokenUsage();

    @Select("SELECT DATE(create_time) AS day, COUNT(*) AS cnt FROM ai_message WHERE role = 'ASSISTANT' AND create_time >= #{since} GROUP BY DATE(create_time) ORDER BY day ASC")
    java.util.List<java.util.Map<String, Object>> dailyMessageCount(@Param("since") String since);

    @Select("SELECT DATE(create_time) AS day, COUNT(*) AS cnt FROM ai_conversation WHERE create_time >= #{since} GROUP BY DATE(create_time) ORDER BY day ASC")
    java.util.List<java.util.Map<String, Object>> dailyConversationCount(@Param("since") String since);

    @Select("SELECT COALESCE(AVG(retrieval_score), 0) FROM ai_message WHERE role = 'ASSISTANT' AND retrieval_score IS NOT NULL")
    double avgRetrievalScore();

    @Select("SELECT COALESCE(AVG(retrieval_hit_count), 0) FROM ai_message WHERE role = 'ASSISTANT' AND retrieval_hit_count IS NOT NULL")
    double avgRetrievalHitCount();

    @Select("SELECT COUNT(*) FROM ai_message WHERE role = 'ASSISTANT' AND retrieval_hit_count IS NOT NULL AND retrieval_hit_count > 0")
    long countWithHits();

    @Select("SELECT COUNT(*) FROM ai_message WHERE role = 'ASSISTANT' AND (retrieval_hit_count IS NULL OR retrieval_hit_count = 0)")
    long countWithoutHits();

    @Delete("DELETE FROM ai_message WHERE conversation_id = #{conversationId}")
    int deleteByConversationId(Long conversationId);

    @Select("SELECT * FROM ai_message WHERE conversation_id = #{conversationId} AND id > #{afterId} ORDER BY create_time ASC")
    List<AiMessage> findAfterId(@Param("conversationId") Long conversationId, @Param("afterId") Long afterId);
}
