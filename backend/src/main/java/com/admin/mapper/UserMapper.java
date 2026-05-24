package com.admin.mapper;

import com.admin.entity.User;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsername(String username);
    
    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    User findById(Long id);
    
    @Select("SELECT * FROM sys_user ORDER BY create_time DESC")
    List<User> findAll();
    
    @Insert("INSERT INTO sys_user (username, password, nickname, email, phone, avatar, status) " +
            "VALUES (#{username}, #{password}, #{nickname}, #{email}, #{phone}, #{avatar}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    
    @Update("UPDATE sys_user SET nickname = #{nickname}, email = #{email}, phone = #{phone}, " +
            "avatar = #{avatar}, status = #{status} WHERE id = #{id}")
    int update(User user);
    
    @Delete("DELETE FROM sys_user WHERE id = #{id}")
    int deleteById(Long id);

    @Update("UPDATE sys_user SET password = #{newPassword} WHERE username = #{username}")
    int updatePassword(@Param("username") String username, @Param("newPassword") String newPassword);

    @Update("UPDATE sys_user SET nickname = #{nickname}, email = #{email}, phone = #{phone}, avatar = #{avatar} WHERE username = #{username}")
    int updateProfile(User user);

    @Select("<script>" +
            "SELECT COUNT(*) FROM sys_user" +
            "<where>" +
            "  <if test='username != null and username != \"\"'> AND (username LIKE CONCAT('%',#{username},'%') OR nickname LIKE CONCAT('%',#{username},'%'))</if>" +
            "  <if test='status != null'> AND status = #{status}</if>" +
            "</where>" +
            "</script>")
    long countByCondition(@Param("username") String username, @Param("status") Integer status);

    @Select("<script>" +
            "SELECT * FROM sys_user" +
            "<where>" +
            "  <if test='username != null and username != \"\"'> AND (username LIKE CONCAT('%',#{username},'%') OR nickname LIKE CONCAT('%',#{username},'%'))</if>" +
            "  <if test='status != null'> AND status = #{status}</if>" +
            "</where>" +
            " ORDER BY create_time DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<User> findByPage(@Param("username") String username, @Param("status") Integer status,
                          @Param("offset") int offset, @Param("limit") int limit);
}
