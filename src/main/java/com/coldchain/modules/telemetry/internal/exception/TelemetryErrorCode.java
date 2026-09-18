package com.coldchain.modules.telemetry.internal.exception;

import com.coldchain.shared.error.ErrorCategory;
import com.coldchain.shared.error.ErrorCode;

public enum TelemetryErrorCode implements ErrorCode {

    DEVICE_NOT_FOUND("error.telemetry.device_not_found", ErrorCategory.NOT_FOUND,
            "The device does not exist"),
    DEVICE_NOT_USABLE("error.telemetry.device_not_usable", ErrorCategory.BUSINESS_RULE,
            "The device is retired or under maintenance"),
    DEVICE_ALREADY_ASSIGNED("error.telemetry.device_already_assigned", ErrorCategory.CONFLICT,
            "That device is already attached to another shipment"),
    DEVICE_NOT_ASSIGNED("error.telemetry.device_not_assigned", ErrorCategory.NOT_FOUND,
            "That device is not attached to anything"),
    SERIAL_ALREADY_REGISTERED("error.telemetry.serial_already_registered", ErrorCategory.CONFLICT,
            "Another device already uses that serial number"),
    EMPTY_BATCH("error.telemetry.empty_batch", ErrorCategory.VALIDATION,
            "A batch with no readings says nothing");

    private final String messageKey;

    private final ErrorCategory category;

    private final String title;

    TelemetryErrorCode(String messageKey, ErrorCategory category, String title) {
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
