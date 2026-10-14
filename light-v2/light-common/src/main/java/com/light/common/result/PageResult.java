package com.light.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 分页响应，避免每个微服务重复定义分页结构。 */
@Data
public class PageResult<T> implements Serializable {
    private List<T> records;
    private long total;
    private long current;
    private long size;
    private long pages;

    public static <T> PageResult<T> of(List<T> records, long total, long current, long size, long pages) {
        PageResult<T> result = new PageResult<>();
        result.records = records;
        result.total = total;
        result.current = current;
        result.size = size;
        result.pages = pages;
        return result;
    }
}
