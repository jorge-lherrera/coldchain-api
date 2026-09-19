package com.coldchain.shared.pagination;

import java.util.List;
import java.util.function.Function;

public record PagedResult<T>(List<T> content, int page, int size, long totalElements,
        List<SortOrder> sort) {

    public PagedResult {
        content = List.copyOf(content);
        sort = List.copyOf(sort);
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public boolean first() {
        return page == 0;
    }

    public boolean last() {
        return page >= totalPages() - 1;
    }

    public <R> PagedResult<R> map(Function<T, R> mapper) {
        return new PagedResult<>(content.stream().map(mapper).toList(), page, size, totalElements, sort);
    }
}
