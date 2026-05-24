package com.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiModelConfig {
    private Long id;
    private String modelKey;
    private String modelName;
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String modelId;
    private Double temperature;
    private Integer maxTokens;
    private Boolean enabled;
    private Boolean isDefault;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
