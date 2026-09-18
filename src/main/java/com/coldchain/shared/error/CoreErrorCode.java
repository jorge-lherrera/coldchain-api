package com.coldchain.shared.error;

public enum CoreErrorCode implements ErrorCode {

    VALIDATION_FAILED("error.core.validation_failed", ErrorCategory.VALIDATION,
            "The request body did not pass validation"),
    MALFORMED_REQUEST("error.core.malformed_request", ErrorCategory.VALIDATION,
            "The request could not be read"),
    UNSORTABLE_FIELD("error.core.unsortable_field", ErrorCategory.VALIDATION,
            "The requested sort field is not sortable on this endpoint"),
    NOT_AUTHENTICATED("error.core.not_authenticated", ErrorCategory.AUTHENTICATION,
            "The request carried no usable credentials"),
    NOT_AUTHORIZED("error.core.not_authorized", ErrorCategory.AUTHORIZATION,
            "The credentials do not grant this operation"),
    RESOURCE_NOT_FOUND("error.core.resource_not_found", ErrorCategory.NOT_FOUND,
            "The resource does not exist or is not visible"),
    CONCURRENT_MODIFICATION("error.core.concurrent_modification", ErrorCategory.CONFLICT,
            "The resource changed while this request was being processed"),
    TOO_MANY_REQUESTS("error.core.too_many_requests", ErrorCategory.RATE_LIMIT,
            "The client sent too many requests in a short time"),
    UNEXPECTED_FAILURE("error.core.unexpected_failure", ErrorCategory.INTERNAL,
            "The request could not be completed");

    private final String messageKey;

    private final ErrorCategory category;

    private final String title;

    CoreErrorCode(String messageKey, ErrorCategory category, String title) {
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
