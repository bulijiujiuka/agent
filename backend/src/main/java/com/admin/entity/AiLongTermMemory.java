package com.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiLongTermMemory {
    private Long id;
    private Long userId;
    private Long conversationId;
    private Long messageId;
    private String businessType;
    private String memoryType;
    private String memoryText;
    private String memorySummary;
    private String sourceType;
    private Long sourceRefId;
    private Double importanceScore;
    private Double confidenceScore;
    private Integer accessCount;
    private String embeddingStatus;
    private String vectorId;
    private Boolean enabled;
    private LocalDateTime lastAccessTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
