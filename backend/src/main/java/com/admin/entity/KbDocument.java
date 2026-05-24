package com.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KbDocument {
    private Long id;
    private Long fileId;
    private String documentName;
    private String sourceType;
    private String category;
    private String tags;
    private String contentType;
    private String parseStatus;
    private String embeddingStatus;
    private Integer chunkCount;
    private Boolean enabled;
    private Integer version;
    private String summary;
    private String uploadedBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
