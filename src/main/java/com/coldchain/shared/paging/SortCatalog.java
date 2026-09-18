package com.coldchain.shared.paging;

import com.coldchain.shared.error.CoreErrorCode;
import com.coldchain.shared.error.DomainException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class SortCatalog<F extends Enum<F> & SortField> {

    private final Map<String, F> byParameter;

    private final Sort fallback;

    private SortCatalog(Map<String, F> byParameter, Sort fallback) {
        this.byParameter = byParameter;
        this.fallback = fallback;
    }

    public static <F extends Enum<F> & SortField> SortCatalog<F> of(Class<F> type, F fallback,
            Sort.Direction direction) {
        Map<String, F> byParameter = EnumSet.allOf(type).stream()
                .collect(Collectors.toUnmodifiableMap(SortField::parameter, Function.identity()));
        return new SortCatalog<>(byParameter, Sort.by(direction, fallback.property()));
    }

    public PageCriteria apply(Pageable requested) {
        return new PageCriteria(requested.getPageNumber(), requested.getPageSize(),
                resolve(requested.getSort()));
    }

    public List<SortOrder> resolve(Sort requested) {
        Sort resolved = requested.isUnsorted() ? fallback
                : Sort.by(requested.stream().map(this::translate).toList());
        return resolved.stream()
                .map(order -> new SortOrder(order.getProperty(),
                        order.isDescending() ? SortDirection.DESC : SortDirection.ASC))
                .toList();
    }

    private Sort.Order translate(Sort.Order order) {
        F field = byParameter.get(order.getProperty());
        if (field == null) {
            throw DomainException.of(CoreErrorCode.UNSORTABLE_FIELD,
                    "This endpoint cannot sort by \"" + order.getProperty() + "\". Sortable fields are "
                            + String.join(", ", byParameter.keySet().stream().sorted().toList()));
        }
        return new Sort.Order(order.getDirection(), field.property());
    }
}
