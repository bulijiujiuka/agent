package com.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {

    /** 存储类型：LOCAL / OSS */
    private String storageType = "LOCAL";

    /** 本地存储根路径 */
    private String localPath = "./uploads";

    /** 文件访问URL前缀 */
    private String urlPrefix = "/uploads";

    /** 最大文件大小（MB） */
    private long maxSize = 50;

    /** 允许的文件扩展名 */
    private List<String> allowedExtensions = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "webp",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "txt", "md", "csv",
        "zip", "rar", "7z",
        "mp4", "mp3"
    );
}
