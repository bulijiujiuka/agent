package com.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiPromptConfig {
    private Long id;
    private String configKey;
    private String configName;
    private String content;
    private String description;
    private Boolean enabled;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
