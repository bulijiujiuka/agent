package com.admin.mapper;

import com.admin.entity.AiCallLog;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface AiCallLogMapper {

    @Insert("INSERT INTO ai_call_log (trace_id, conversation_id, message_id, step_name, model_key, " +
            "input_length, output_length, duration_ms, success, error_msg, extra) " +
            "VALUES (#{traceId}, #{conversationId}, #{messageId}, #{stepName}, #{modelKey}, " +
            "#{inputLength}, #{outputLength}, #{durationMs}, #{success}, #{errorMsg}, #{extra})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiCallLog log);

    @Select("SELECT * FROM ai_call_log WHERE trace_id = #{traceId} ORDER BY id ASC")
    List<AiCallLog> findByTraceId(String traceId);

    @Select("SELECT * FROM ai_call_log WHERE conversation_id = #{conversationId} ORDER BY id DESC")
    List<AiCallLog> findByConversationId(Long conversationId);

    @Select("<script>" +
            "SELECT * FROM ai_call_log" +
            "<where>" +
            "  <if test='stepName != null and stepName != \"\"'> AND step_name = #{stepName}</if>" +
            "  <if test='success != null'> AND success = #{success}</if>" +
            "</where>" +
            " ORDER BY id DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<AiCallLog> findByPage(@Param("stepName") String stepName,
                                @Param("success") Boolean success,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM ai_call_log" +
            "<where>" +
            "  <if test='stepName != null and stepName != \"\"'> AND step_name = #{stepName}</if>" +
            "  <if test='success != null'> AND success = #{success}</if>" +
            "</where>" +
            "</script>")
    long countByCondition(@Param("stepName") String stepName, @Param("success") Boolean success);

    @Select("SELECT step_name AS stepName, COUNT(*) AS cnt, AVG(duration_ms) AS avgMs, " +
            "SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) AS failCount " +
            "FROM ai_call_log WHERE create_time >= #{since} GROUP BY step_name")
    List<Map<String, Object>> statsByStep(@Param("since") String since);
}
