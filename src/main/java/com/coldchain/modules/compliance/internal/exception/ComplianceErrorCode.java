package com.coldchain.modules.compliance.internal.exception;

import com.coldchain.shared.error.ErrorCategory;
import com.coldchain.shared.error.ErrorCode;

public enum ComplianceErrorCode implements ErrorCode {

    CERTIFICATE_NOT_FOUND("error.compliance.certificate_not_found", ErrorCategory.NOT_FOUND,
            "That shipment has no certificate yet"),
    SHIPMENT_NOT_READY("error.compliance.shipment_not_ready", ErrorCategory.BUSINESS_RULE,
            "A shipment that never left has nothing to certify"),
    THRESHOLDS_NOT_FROZEN("error.compliance.thresholds_not_frozen", ErrorCategory.BUSINESS_RULE,
            "The shipment froze no thresholds, so there is no promise to judge against");

    private final String messageKey;

    private final ErrorCategory category;

    private final String title;

    ComplianceErrorCode(String messageKey, ErrorCategory category, String title) {
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
