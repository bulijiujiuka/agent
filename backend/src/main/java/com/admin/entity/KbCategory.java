package com.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KbCategory {
    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
