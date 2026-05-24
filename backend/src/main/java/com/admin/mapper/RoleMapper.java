package com.admin.mapper;

import com.admin.entity.Role;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RoleMapper {
    
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Role> findByUserId(Long userId);
    
    @Select("SELECT * FROM sys_role ORDER BY create_time DESC")
    List<Role> findAll();
    
    @Select("SELECT * FROM sys_role WHERE id = #{id}")
    Role findById(Long id);
    
    @Insert("INSERT INTO sys_role (role_name, role_code, description, status) " +
            "VALUES (#{roleName}, #{roleCode}, #{description}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Role role);
    
    @Update("UPDATE sys_role SET role_name = #{roleName}, description = #{description}, " +
            "status = #{status} WHERE id = #{id}")
    int update(Role role);
    
    @Delete("DELETE FROM sys_role WHERE id = #{id}")
    int deleteById(Long id);
}
