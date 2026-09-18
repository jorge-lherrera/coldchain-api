package com.coldchain.modules.catalog.internal.exception;

import com.coldchain.shared.error.ErrorCategory;
import com.coldchain.shared.error.ErrorCode;

public enum CatalogErrorCode implements ErrorCode {

    PROFILE_NOT_FOUND("error.catalog.profile_not_found", ErrorCategory.NOT_FOUND,
            "The storage profile does not exist"),
    PRODUCT_NOT_FOUND("error.catalog.product_not_found", ErrorCategory.NOT_FOUND,
            "The product does not exist"),
    SITE_NOT_FOUND("error.catalog.site_not_found", ErrorCategory.NOT_FOUND,
            "The site does not exist"),
    PROFILE_CODE_ALREADY_IN_USE("error.catalog.profile_code_already_in_use", ErrorCategory.CONFLICT,
            "Another storage profile already uses that code"),
    SKU_ALREADY_IN_USE("error.catalog.sku_already_in_use", ErrorCategory.CONFLICT,
            "Another product already uses that SKU"),
    SITE_CODE_ALREADY_IN_USE("error.catalog.site_code_already_in_use", ErrorCategory.CONFLICT,
            "Another site already uses that code"),
    PROFILE_IN_USE_CLONE_INSTEAD("error.catalog.profile_in_use_clone_instead", ErrorCategory.BUSINESS_RULE,
            "The profile is in use: clone it to a new version instead of editing it"),
    PROFILE_NOT_USABLE("error.catalog.profile_not_usable", ErrorCategory.BUSINESS_RULE,
            "Only an active storage profile can be assigned to a product"),
    PROFILE_ALREADY_ACTIVE("error.catalog.profile_already_active", ErrorCategory.BUSINESS_RULE,
            "The storage profile is already active"),
    INVALID_THRESHOLDS("error.catalog.invalid_thresholds", ErrorCategory.VALIDATION,
            "The thresholds do not describe a usable range"),
    INVALID_TIME_ZONE("error.catalog.invalid_time_zone", ErrorCategory.VALIDATION,
            "A site keeps an IANA zone identifier, not an offset");

    private final String messageKey;

    private final ErrorCategory category;

    private final String title;

    CatalogErrorCode(String messageKey, ErrorCategory category, String title) {
        this.messageKey = messageKey;
        this.category = category;
        this.title = title;
    }

    @Override
    public String messageKey() {
        return messageKey;
    }

    @Override
    public ErrorCategory category() {
        return category;
    }

    @Override
    public String title() {
        return title;
    }
}
