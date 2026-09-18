package com.coldchain.delivery.web.catalog;

import com.coldchain.shared.response.SuccessCode;
import com.coldchain.shared.response.SuccessOutcome;

public enum CatalogSuccessCode implements SuccessCode {

    PROFILE_CREATED("success.catalog.profile_created", SuccessOutcome.CREATED, "Storage profile created"),
    PROFILE_UPDATED("success.catalog.profile_updated", SuccessOutcome.UPDATED, "Storage profile updated"),
    PROFILE_ACTIVATED("success.catalog.profile_activated", SuccessOutcome.UPDATED,
            "Storage profile activated"),
    PROFILE_CLONED("success.catalog.profile_cloned", SuccessOutcome.CREATED,
            "Storage profile cloned to the next version"),
    PROFILE_LISTED("success.catalog.profile_listed", SuccessOutcome.RETRIEVED,
            "Storage profiles retrieved"),
    PRODUCT_CREATED("success.catalog.product_created", SuccessOutcome.CREATED, "Product created"),
    PRODUCT_UPDATED("success.catalog.product_updated", SuccessOutcome.UPDATED, "Product updated"),
    PRODUCT_RETIRED("success.catalog.product_retired", SuccessOutcome.DELETED, "Product retired"),
    PRODUCT_LISTED("success.catalog.product_listed", SuccessOutcome.RETRIEVED, "Products retrieved"),
    SITE_CREATED("success.catalog.site_created", SuccessOutcome.CREATED, "Site created"),
    SITE_UPDATED("success.catalog.site_updated", SuccessOutcome.UPDATED, "Site updated"),
    SITE_LISTED("success.catalog.site_listed", SuccessOutcome.RETRIEVED, "Sites retrieved");

    private final String messageKey;

    private final SuccessOutcome outcome;

    private final String message;

    CatalogSuccessCode(String messageKey, SuccessOutcome outcome, String message) {
        this.messageKey = messageKey;
        this.outcome = outcome;
        this.message = message;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }

    @Override
    public SuccessOutcome outcome() {
        return outcome;
    }

    @Override
    public String message() {
        return message;
    }
}
