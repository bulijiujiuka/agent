package com.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiConversation {
    private Long id;
    private String conversationNo;
    private String title;
    private String businessType;
    private Long userId;
    private String modelName;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    //摘要字段
    private String summary;    // 会话摘要
    private Long summaryMessageSeq;    // 摘要消息序号
}
