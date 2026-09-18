package com.coldchain.delivery.web.catalog;

import com.coldchain.shared.paging.SortField;

public enum StorageProfileSortField implements SortField {

    CODE("code", "code"),
    NAME("name", "name"),
    VERSION("version", "profileVersion"),
    STATUS("status", "status");

    private final String parameter;

    private final String property;

    StorageProfileSortField(String parameter, String property) {
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
