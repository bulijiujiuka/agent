package com.admin.mapper;

import com.admin.entity.KbCategory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KbCategoryMapper {

    @Select("SELECT * FROM kb_category ORDER BY sort_order ASC")
    List<KbCategory> findAll();

    @Select("SELECT * FROM kb_category WHERE id = #{id}")
    KbCategory findById(Long id);

    @Insert("INSERT INTO kb_category (name, description, sort_order) VALUES (#{name}, #{description}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KbCategory category);

    @Update("UPDATE kb_category SET name = #{name}, description = #{description}, sort_order = #{sortOrder} WHERE id = #{id}")
    int update(KbCategory category);

    @Delete("DELETE FROM kb_category WHERE id = #{id}")
    int deleteById(Long id);
}
