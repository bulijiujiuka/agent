package com.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SensitiveWord {
    private Long id;
    private String word;
    private String category;
    private String replacement;
    private Boolean enabled;
    private LocalDateTime createTime;
}
