package com.coldchain.delivery.web.advisory;

import com.coldchain.shared.response.SuccessCode;
import com.coldchain.shared.response.SuccessOutcome;

public enum AdvisorySuccessCode implements SuccessCode {

    ALERTS_RETRIEVED("success.advisory.alerts_retrieved", SuccessOutcome.RETRIEVED, "Alerts retrieved");

    private final String messageKey;

    private final SuccessOutcome outcome;

    private final String message;

    AdvisorySuccessCode(String messageKey, SuccessOutcome outcome, String message) {
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
