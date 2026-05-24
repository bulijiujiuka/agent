package com.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatMemory {
    private Long id;
    private Long userId;
    private Long conversationId;
    private String role;
    private String content;
    private Integer seq;
    private LocalDateTime createTime;
}
