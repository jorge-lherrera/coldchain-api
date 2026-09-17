package com.coldchain.modules.identity.internal.exception;

import com.coldchain.shared.error.ErrorCategory;
import com.coldchain.shared.error.ErrorCode;

public enum IdentityErrorCode implements ErrorCode {

    TAX_ID_ALREADY_REGISTERED("error.identity.tax_id_already_registered", ErrorCategory.CONFLICT,
            "An organization with that tax identifier already exists"),
    EMAIL_ALREADY_REGISTERED("error.identity.email_already_registered", ErrorCategory.CONFLICT,
            "That email address is already registered"),
    CLIENT_ID_ALREADY_REGISTERED("error.identity.client_id_already_registered", ErrorCategory.CONFLICT,
            "That client identifier is already taken"),
    ORGANIZATION_NOT_FOUND("error.identity.organization_not_found", ErrorCategory.NOT_FOUND,
            "The organization does not exist"),
    USER_NOT_FOUND("error.identity.user_not_found", ErrorCategory.NOT_FOUND,
            "The user does not exist"),
    ROLE_NOT_FOUND("error.identity.role_not_found", ErrorCategory.NOT_FOUND,
            "The role does not exist"),
    INVALID_CREDENTIALS("error.identity.invalid_credentials", ErrorCategory.AUTHENTICATION,
            "The email address and password do not match"),
    INVALID_CLIENT_CREDENTIALS("error.identity.invalid_client_credentials", ErrorCategory.AUTHENTICATION,
            "The client identifier and secret do not match"),
    USER_NOT_ACTIVE("error.identity.user_not_active", ErrorCategory.AUTHENTICATION,
            "The user has not been activated"),
    ORGANIZATION_SUSPENDED("error.identity.organization_suspended", ErrorCategory.AUTHORIZATION,
            "The organization is suspended"),
    ACTIVATION_TOKEN_INVALID("error.identity.activation_token_invalid", ErrorCategory.BUSINESS_RULE,
            "The activation link is not valid"),
    ACTIVATION_TOKEN_EXPIRED("error.identity.activation_token_expired", ErrorCategory.BUSINESS_RULE,
            "The activation link has expired"),
    USER_ALREADY_ACTIVE("error.identity.user_already_active", ErrorCategory.BUSINESS_RULE,
            "The user is already active"),
    REFRESH_TOKEN_INVALID("error.identity.refresh_token_invalid", ErrorCategory.AUTHENTICATION,
            "The refresh token is not valid"),
    REFRESH_TOKEN_EXPIRED("error.identity.refresh_token_expired", ErrorCategory.AUTHENTICATION,
            "The refresh token has expired"),
    REFRESH_TOKEN_REUSED("error.identity.refresh_token_reused", ErrorCategory.AUTHENTICATION,
            "The refresh token was already used, so its whole family has been revoked"),
    ROLE_ALREADY_GRANTED("error.identity.role_already_granted", ErrorCategory.BUSINESS_RULE,
            "The user already holds that role"),
    ROLE_NOT_GRANTED("error.identity.role_not_granted", ErrorCategory.BUSINESS_RULE,
            "The user does not hold that role"),
    LAST_ADMINISTRATOR("error.identity.last_administrator", ErrorCategory.BUSINESS_RULE,
            "An organization cannot be left without an administrator"),
    SCOPE_NOT_ALLOWED_FOR_CLIENT("error.identity.scope_not_allowed_for_client", ErrorCategory.BUSINESS_RULE,
            "A machine credential may only carry ingestion scopes");

    private final String messageKey;

    private final ErrorCategory category;

    private final String title;

    IdentityErrorCode(String messageKey, ErrorCategory category, String title) {
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
