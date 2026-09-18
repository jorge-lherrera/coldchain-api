package com.coldchain.shared.response;

import com.coldchain.shared.paging.PagedResult;
import com.coldchain.shared.paging.SortOrder;
import java.util.List;
import java.util.stream.Collectors;

public record PaginationMeta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort) {

    public static PaginationMeta of(PagedResult<?> page) {
        return new PaginationMeta(
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.first(),
                page.last(),
                describe(page.sort()));
    }

    private static String describe(List<SortOrder> sort) {
        return sort.stream()
                .map(order -> order.property() + ": " + order.direction())
                .collect(Collectors.joining(", "));
    }
}
