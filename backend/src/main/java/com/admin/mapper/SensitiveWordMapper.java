package com.admin.mapper;

import com.admin.entity.SensitiveWord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SensitiveWordMapper {

    @Select("SELECT * FROM sensitive_word ORDER BY id ASC")
    List<SensitiveWord> findAll();

    @Select("SELECT * FROM sensitive_word WHERE enabled = 1")
    List<SensitiveWord> findAllEnabled();

    @Select("SELECT * FROM sensitive_word WHERE id = #{id}")
    SensitiveWord findById(Long id);

    @Insert("INSERT INTO sensitive_word (word, category, replacement, enabled) VALUES (#{word}, #{category}, #{replacement}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SensitiveWord word);

    @Update("UPDATE sensitive_word SET word = #{word}, category = #{category}, replacement = #{replacement}, enabled = #{enabled} WHERE id = #{id}")
    int update(SensitiveWord word);

    @Delete("DELETE FROM sensitive_word WHERE id = #{id}")
    int deleteById(Long id);
}
