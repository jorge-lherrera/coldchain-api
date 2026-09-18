package com.coldchain.delivery.web.catalog;

import com.coldchain.shared.paging.SortField;

public enum SiteSortField implements SortField {

    CODE("code", "code"),
    NAME("name", "name"),
    KIND("kind", "kind");

    private final String parameter;

    private final String property;

    SiteSortField(String parameter, String property) {
        this.parameter = parameter;
        this.property = property;
    }

    @Override
    public String parameter() {
        return parameter;
    }

    @Override
    public String property() {
        return property;
    }
}
