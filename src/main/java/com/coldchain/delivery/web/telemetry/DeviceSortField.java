package com.coldchain.delivery.web.telemetry;

import com.coldchain.shared.paging.SortField;

public enum DeviceSortField implements SortField {

    SERIAL_NUMBER("serialNumber", "serialNumber"),
    MODEL("model", "model"),
    STATUS("status", "status");

    private final String parameter;

    private final String property;

    DeviceSortField(String parameter, String property) {
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
