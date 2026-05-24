package com.admin.mapper;

import com.admin.entity.FileInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FileMapper {

    @Insert("INSERT INTO sys_file (original_name, stored_name, file_path, file_url, file_size, " +
            "file_type, file_ext, storage_type, upload_user, biz_type) " +
            "VALUES (#{originalName}, #{storedName}, #{filePath}, #{fileUrl}, #{fileSize}, " +
            "#{fileType}, #{fileExt}, #{storageType}, #{uploadUser}, #{bizType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FileInfo fileInfo);

    @Select("SELECT * FROM sys_file WHERE id = #{id}")
    FileInfo findById(Long id);

    @Select("SELECT * FROM sys_file WHERE biz_type = #{bizType} ORDER BY create_time DESC")
    List<FileInfo> findByBizType(String bizType);

    @Select("SELECT * FROM sys_file WHERE upload_user = #{uploadUser} ORDER BY create_time DESC")
    List<FileInfo> findByUploadUser(String uploadUser);

    @Select("SELECT * FROM sys_file ORDER BY create_time DESC")
    List<FileInfo> findAll();

    @Delete("DELETE FROM sys_file WHERE id = #{id}")
    int deleteById(Long id);
}
