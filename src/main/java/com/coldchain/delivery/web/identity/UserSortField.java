package com.coldchain.delivery.web.identity;

import com.coldchain.shared.paging.SortField;

public enum UserSortField implements SortField {

    FULL_NAME("fullName", "fullName"),
    EMAIL("email", "email"),
    STATUS("status", "status"),
    LAST_LOGIN_AT("lastLoginAt", "lastLoginAt");

    private final String parameter;

    private final String property;

    UserSortField(String parameter, String property) {
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
