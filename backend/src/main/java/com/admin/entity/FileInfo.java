package com.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FileInfo {
    private Long id;
    private String originalName;
    private String storedName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private String fileExt;
    private String storageType;
    private String uploadUser;
    private String bizType;
    private LocalDateTime createTime;
}
