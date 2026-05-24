package com.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KbDocumentVersion {
    private Long id;
    private Long documentId;
    private Integer version;
    private String documentName;
    private String category;
    private String tags;
    private String summary;
    private String contentSnapshot;
    private String createdBy;
    private LocalDateTime createTime;
}
