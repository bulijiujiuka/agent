package com.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Ticket {
    private Long id;
    private String ticketNo;
    private String title;
    private String content;
    private String customerName;
    private String category;
    private String priority;
    private String ticketStatus;
    private String aiSummary;
    private String aiReply;
    private String aiSuggestion;
    private String assignee;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
