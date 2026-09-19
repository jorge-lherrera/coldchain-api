package com.coldchain.shared.pagination;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class SpringDataPaging {

    private SpringDataPaging() {
    }

    public static Pageable toPageable(PageCriteria criteria) {
        return PageRequest.of(criteria.page(), criteria.size(), toSort(criteria.sort()));
    }

    public static <T> PagedResult<T> toPagedResult(Page<T> page, PageCriteria criteria) {
        return new PagedResult<>(page.getContent(), criteria.page(), criteria.size(),
                page.getTotalElements(), criteria.sort());
    }

    private static Sort toSort(List<SortOrder> orders) {
        return Sort.by(orders.stream()
                .map(order -> new Sort.Order(
                        order.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC,
                        order.property()))
                .toList());
    }
}
