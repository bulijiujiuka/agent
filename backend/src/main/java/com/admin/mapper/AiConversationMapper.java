package com.admin.mapper;

import com.admin.entity.AiConversation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiConversationMapper {

    @Select("SELECT * FROM ai_conversation WHERE user_id = #{userId} ORDER BY update_time DESC")
    List<AiConversation> findByUserId(Long userId);

    @Select("SELECT * FROM ai_conversation WHERE id = #{id}")
    AiConversation findById(Long id);

    @Select("SELECT * FROM ai_conversation WHERE conversation_no = #{conversationNo}")
    AiConversation findByConversationNo(String conversationNo);

    @Insert("INSERT INTO ai_conversation (conversation_no, title, business_type, user_id, model_name, status) " +
            "VALUES (#{conversationNo}, #{title}, #{businessType}, #{userId}, #{modelName}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiConversation conversation);

    @Update("UPDATE ai_conversation SET title = #{title}, status = #{status} WHERE id = #{id}")
    int update(AiConversation conversation);

    @Delete("DELETE FROM ai_conversation WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM ai_conversation")
    long countAll();

    @Update("UPDATE ai_conversation SET summary = #{summary},summary_message_seq = #{summaryMessageSeq} WHERE id = #{id}")
    int updateSummary(AiConversation conversation);

    @Select("<script>" +
            "SELECT * FROM ai_conversation" +
            "<where>" +
            "  <if test='userId != null'> AND user_id = #{userId}</if>" +
            "  <if test='status != null and status != \"\"'> AND status = #{status}</if>" +
            "</where>" +
            " ORDER BY update_time DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    java.util.List<AiConversation> findByPage(@Param("userId") Long userId,
                                               @Param("status") String status,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM ai_conversation" +
            "<where>" +
            "  <if test='userId != null'> AND user_id = #{userId}</if>" +
            "  <if test='status != null and status != \"\"'> AND status = #{status}</if>" +
            "</where>" +
            "</script>")
    long countByCondition(@Param("userId") Long userId, @Param("status") String status);
}
