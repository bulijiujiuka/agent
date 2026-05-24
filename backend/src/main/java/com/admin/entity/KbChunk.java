package com.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KbChunk {
    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String chunkText;
    private Integer charCount;
    private String vectorStatus;
    private LocalDateTime createTime;
    private String sectionTitle;
    private String titlePath;
}
