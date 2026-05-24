package com.admin.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用分页返回结果
 */
@Data
public class PageResult<T> {

    /** 总记录数 */
    private Long total;

    /** 当前页数据 */
    private List<T> records;

    /** 当前页码 */
    private Integer pageNum;

    /** 每页条数 */
    private Integer pageSize;

    /** 总页数 */
    private Integer pages;

    public PageResult(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = (int) Math.ceil((double) total / pageSize);
    }

    public static <T> PageResult<T> of(List<T> records, Long total, PageRequest pageRequest) {
        return new PageResult<>(records, total, pageRequest.getPageNum(), pageRequest.getPageSize());
    }
}
