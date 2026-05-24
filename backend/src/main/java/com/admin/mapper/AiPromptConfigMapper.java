package com.admin.mapper;

import com.admin.entity.AiPromptConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiPromptConfigMapper {

    @Select("SELECT * FROM ai_prompt_config ORDER BY sort_order ASC")
    List<AiPromptConfig> findAll();

    @Select("SELECT * FROM ai_prompt_config WHERE id = #{id}")
    AiPromptConfig findById(Long id);

    @Select("SELECT * FROM ai_prompt_config WHERE config_key = #{configKey}")
    AiPromptConfig findByKey(String configKey);

    @Select("SELECT * FROM ai_prompt_config WHERE enabled = 1 ORDER BY sort_order ASC")
    List<AiPromptConfig> findAllEnabled();

    @Insert("INSERT INTO ai_prompt_config (config_key, config_name, content, description, enabled, sort_order) " +
            "VALUES (#{configKey}, #{configName}, #{content}, #{description}, #{enabled}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiPromptConfig config);

    @Update("<script>" +
            "UPDATE ai_prompt_config <set>" +
            "  <if test='configName != null'> config_name = #{configName}, </if>" +
            "  <if test='content != null'> content = #{content}, </if>" +
            "  <if test='description != null'> description = #{description}, </if>" +
            "  <if test='enabled != null'> enabled = #{enabled}, </if>" +
            "  <if test='sortOrder != null'> sort_order = #{sortOrder}, </if>" +
            "</set> WHERE id = #{id}" +
            "</script>")
    int update(AiPromptConfig config);

    @Delete("DELETE FROM ai_prompt_config WHERE id = #{id}")
    int deleteById(Long id);
}
