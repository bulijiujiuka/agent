package com.admin.dto;

import lombok.Data;

/**
 * 通用分页请求参数
 */
@Data
public class PageRequest {

    /** 当前页码（从1开始） */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    /**
     * 计算 SQL OFFSET
     */
    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
