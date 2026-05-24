package com.admin.mapper;

import com.admin.entity.AiLongTermMemory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiLongTermMemoryMapper {

    @Select("SELECT * FROM ai_long_term_memory WHERE id = #{id}")
    AiLongTermMemory findById(Long id);

    @Select("SELECT * FROM ai_long_term_memory WHERE enabled = 1 ORDER BY create_time DESC")
    List<AiLongTermMemory> findAllEnabled();

    @Select("""
            SELECT * FROM ai_long_term_memory
            WHERE enabled = 1
              AND embedding_status <> 'COMPLETED'
            ORDER BY create_time ASC
            LIMIT #{limit}
            """)
    List<AiLongTermMemory> findNotEmbedded(@Param("limit") int limit);

    @Select("""
            SELECT * FROM ai_long_term_memory
            WHERE enabled = 1
              AND (#{userId} IS NULL OR user_id = #{userId})
              AND (#{memoryType} IS NULL OR #{memoryType} = '' OR memory_type = #{memoryType})
              AND (#{businessType} IS NULL OR #{businessType} = '' OR business_type = #{businessType})
            ORDER BY importance_score DESC, confidence_score DESC, create_time DESC
            LIMIT #{limit}
            """)
    List<AiLongTermMemory> findCandidates(@Param("userId") Long userId,
                                           @Param("memoryType") String memoryType,
                                           @Param("businessType") String businessType,
                                           @Param("limit") int limit);

    @Select("""
            SELECT * FROM ai_long_term_memory
            WHERE enabled = 1
              AND (#{userId} IS NULL OR user_id = #{userId})
              AND memory_text LIKE CONCAT('%', #{query}, '%')
            ORDER BY importance_score DESC, confidence_score DESC, create_time DESC
            LIMIT #{limit}
            """)
    List<AiLongTermMemory> likeSearch(@Param("query") String query,
                                      @Param("userId") Long userId,
                                      @Param("limit") int limit);

    @Insert("INSERT INTO ai_long_term_memory (user_id, conversation_id, message_id, business_type, memory_type, " +
            "memory_text, memory_summary, source_type, source_ref_id, importance_score, confidence_score, " +
            "access_count, embedding_status, vector_id, enabled, last_access_time) " +
            "VALUES (#{userId}, #{conversationId}, #{messageId}, #{businessType}, #{memoryType}, " +
            "#{memoryText}, #{memorySummary}, #{sourceType}, #{sourceRefId}, #{importanceScore}, #{confidenceScore}, " +
            "#{accessCount}, #{embeddingStatus}, #{vectorId}, #{enabled}, #{lastAccessTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiLongTermMemory memory);

    @Update("UPDATE ai_long_term_memory SET business_type = #{businessType}, memory_type = #{memoryType}, " +
            "memory_text = #{memoryText}, memory_summary = #{memorySummary}, importance_score = #{importanceScore}, " +
            "confidence_score = #{confidenceScore}, embedding_status = #{embeddingStatus}, vector_id = #{vectorId}, " +
            "enabled = #{enabled} WHERE id = #{id}")
    int update(AiLongTermMemory memory);

    @Update("UPDATE ai_long_term_memory SET embedding_status = #{status} WHERE id = #{id}")
    int updateEmbeddingStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE ai_long_term_memory SET embedding_status = #{status}, vector_id = #{vectorId} WHERE id = #{id}")
    int updateVectorStatus(@Param("id") Long id,
                           @Param("status") String status,
                           @Param("vectorId") String vectorId);

    @Update("UPDATE ai_long_term_memory SET access_count = access_count + 1, last_access_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int incrementAccess(Long id);

    @Update("UPDATE ai_long_term_memory SET enabled = #{enabled} WHERE id = #{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);

    @Delete("DELETE FROM ai_long_term_memory WHERE id = #{id}")
    int deleteById(Long id);
}
