package com.admin.mapper;

import com.admin.entity.OperLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OperLogMapper {

    @Insert("INSERT INTO sys_oper_log (module, description, method, request_method, request_url, " +
            "request_params, response_result, status, error_msg, oper_user, oper_ip, cost_time) " +
            "VALUES (#{module}, #{description}, #{method}, #{requestMethod}, #{requestUrl}, " +
            "#{requestParams}, #{responseResult}, #{status}, #{errorMsg}, #{operUser}, #{operIp}, #{costTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperLog operLog);

    @Select("<script>" +
            "SELECT COUNT(*) FROM sys_oper_log" +
            "<where>" +
            "  <if test='module != null and module != \"\"'> AND module = #{module}</if>" +
            "  <if test='operUser != null and operUser != \"\"'> AND oper_user LIKE CONCAT('%',#{operUser},'%')</if>" +
            "</where>" +
            "</script>")
    long count(@Param("module") String module, @Param("operUser") String operUser);

    @Select("<script>" +
            "SELECT * FROM sys_oper_log" +
            "<where>" +
            "  <if test='module != null and module != \"\"'> AND module = #{module}</if>" +
            "  <if test='operUser != null and operUser != \"\"'> AND oper_user LIKE CONCAT('%',#{operUser},'%')</if>" +
            "</where>" +
            " ORDER BY create_time DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<OperLog> findByPage(@Param("module") String module, @Param("operUser") String operUser,
                              @Param("offset") int offset, @Param("limit") int limit);

    @Delete("DELETE FROM sys_oper_log WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("TRUNCATE TABLE sys_oper_log")
    void clear();
}
