package com.admin.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询请求（继承通用分页参数，扩展搜索条件）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryRequest extends PageRequest {

    /** 用户名（模糊搜索） */
    private String username;

    /** 状态筛选 */
    private Integer status;
}
