package com.coldchain.delivery.web.compliance;

import com.coldchain.shared.response.SuccessCode;
import com.coldchain.shared.response.SuccessOutcome;

public enum ComplianceSuccessCode implements SuccessCode {

    CERTIFICATE_ISSUED("success.compliance.certificate_issued", SuccessOutcome.CREATED,
            "Certificate issued"),
    CERTIFICATE_RETRIEVED("success.compliance.certificate_retrieved", SuccessOutcome.RETRIEVED,
            "Certificate retrieved");

    private final String messageKey;

    private final SuccessOutcome outcome;

    private final String message;

    ComplianceSuccessCode(String messageKey, SuccessOutcome outcome, String message) {
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
