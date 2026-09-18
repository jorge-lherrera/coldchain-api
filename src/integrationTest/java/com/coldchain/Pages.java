package com.coldchain;

import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.SortDirection;
import com.coldchain.shared.paging.SortOrder;
import java.util.List;

final class Pages {

    static final PageCriteria FIRST =
            new PageCriteria(0, 50, List.of(new SortOrder("code", SortDirection.ASC)));

    private Pages() {
    }
}
