package com.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiMessage {
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private String referenceContent;
    private Integer tokenUsage;
    private String feedback;
    private Integer responseMs;
    private Double retrievalScore;
    private Integer retrievalHitCount;
    private LocalDateTime createTime;
}
