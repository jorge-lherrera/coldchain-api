package com.coldchain;

import com.coldchain.shared.pagination.PageCriteria;
import com.coldchain.shared.pagination.SortDirection;
import com.coldchain.shared.pagination.SortOrder;
import java.util.List;

final class Pages {

    static final PageCriteria FIRST =
            new PageCriteria(0, 50, List.of(new SortOrder("code", SortDirection.ASC)));

    static final PageCriteria BY_REFERENCE =
            new PageCriteria(0, 50, List.of(new SortOrder("reference", SortDirection.ASC)));

    private Pages() {
    }
}
