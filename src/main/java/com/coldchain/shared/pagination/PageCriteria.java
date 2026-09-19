package com.coldchain.shared.pagination;

import java.util.List;

public record PageCriteria(int page, int size, List<SortOrder> sort) {

    public PageCriteria {
        if (page < 0) {
            throw new IllegalArgumentException("A page number is never negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("A page size is always positive");
        }
        sort = List.copyOf(sort);
    }
}
