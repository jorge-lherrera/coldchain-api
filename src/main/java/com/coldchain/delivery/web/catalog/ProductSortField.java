package com.coldchain.delivery.web.catalog;

import com.coldchain.shared.paging.SortField;

public enum ProductSortField implements SortField {

    SKU("sku", "sku"),
    NAME("name", "name");

    private final String parameter;

    private final String property;

    ProductSortField(String parameter, String property) {
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
