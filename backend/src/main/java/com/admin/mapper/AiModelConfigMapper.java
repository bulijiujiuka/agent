package com.admin.mapper;

import com.admin.entity.AiModelConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiModelConfigMapper {

    @Select("SELECT * FROM ai_model_config ORDER BY sort_order ASC")
    List<AiModelConfig> findAll();

    @Select("SELECT * FROM ai_model_config WHERE id = #{id}")
    AiModelConfig findById(Long id);

    @Select("SELECT * FROM ai_model_config WHERE model_key = #{modelKey}")
    AiModelConfig findByKey(String modelKey);

    @Select("SELECT * FROM ai_model_config WHERE is_default = 1 AND enabled = 1 LIMIT 1")
    AiModelConfig findDefault();

    @Select("SELECT * FROM ai_model_config WHERE enabled = 1 ORDER BY sort_order ASC")
    List<AiModelConfig> findAllEnabled();

    @Insert("INSERT INTO ai_model_config (model_key, model_name, provider, base_url, api_key, model_id, temperature, max_tokens, enabled, is_default, sort_order) " +
            "VALUES (#{modelKey}, #{modelName}, #{provider}, #{baseUrl}, #{apiKey}, #{modelId}, #{temperature}, #{maxTokens}, #{enabled}, #{isDefault}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiModelConfig config);

    @Update("<script>" +
            "UPDATE ai_model_config <set>" +
            "  <if test='modelName != null'> model_name = #{modelName}, </if>" +
            "  <if test='provider != null'> provider = #{provider}, </if>" +
            "  <if test='baseUrl != null'> base_url = #{baseUrl}, </if>" +
            "  <if test='apiKey != null'> api_key = #{apiKey}, </if>" +
            "  <if test='modelId != null'> model_id = #{modelId}, </if>" +
            "  <if test='temperature != null'> temperature = #{temperature}, </if>" +
            "  <if test='maxTokens != null'> max_tokens = #{maxTokens}, </if>" +
            "  <if test='enabled != null'> enabled = #{enabled}, </if>" +
            "  <if test='isDefault != null'> is_default = #{isDefault}, </if>" +
            "  <if test='sortOrder != null'> sort_order = #{sortOrder}, </if>" +
            "</set> WHERE id = #{id}" +
            "</script>")
    int update(AiModelConfig config);

    @Delete("DELETE FROM ai_model_config WHERE id = #{id}")
    int deleteById(Long id);

    @Update("UPDATE ai_model_config SET is_default = 0 WHERE is_default = 1")
    int clearDefault();
}
