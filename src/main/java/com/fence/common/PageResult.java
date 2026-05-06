package com.fence.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private int pageNum;
    private int pageSize;
    private long total;
    private List<T> list;

    public PageResult(long total, List<T> list) {
        this.total = total;
        this.list = list;
    }
}
