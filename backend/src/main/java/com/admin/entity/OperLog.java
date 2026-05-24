package com.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperLog {
    private Long id;
    private String module;
    private String description;
    private String method;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private String responseResult;
    private Integer status;
    private String errorMsg;
    private String operUser;
    private String operIp;
    private Long costTime;
    private LocalDateTime createTime;
}
