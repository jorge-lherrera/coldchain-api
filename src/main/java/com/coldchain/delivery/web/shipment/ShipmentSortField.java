package com.coldchain.delivery.web.shipment;

import com.coldchain.shared.paging.SortField;

public enum ShipmentSortField implements SortField {

    REFERENCE("reference", "reference"),
    STATUS("status", "status"),
    DISPATCHED_AT("dispatchedAt", "dispatchedAt");

    private final String parameter;

    private final String property;

    ShipmentSortField(String parameter, String property) {
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
