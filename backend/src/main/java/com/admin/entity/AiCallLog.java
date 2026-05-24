package com.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiCallLog {
    private Long id;
    private String traceId;
    private Long conversationId;
    private Long messageId;
    private String stepName;
    private String modelKey;
    private Integer inputLength;
    private Integer outputLength;
    private Integer durationMs;
    private Boolean success;
    private String errorMsg;
    private String extra;
    private LocalDateTime createTime;
}
