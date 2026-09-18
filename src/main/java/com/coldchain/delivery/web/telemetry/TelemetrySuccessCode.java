package com.coldchain.delivery.web.telemetry;

import com.coldchain.shared.response.SuccessCode;
import com.coldchain.shared.response.SuccessOutcome;

public enum TelemetrySuccessCode implements SuccessCode {

    DEVICE_REGISTERED("success.telemetry.device_registered", SuccessOutcome.CREATED,
            "Device registered"),
    DEVICE_LISTED("success.telemetry.device_listed", SuccessOutcome.RETRIEVED, "Devices retrieved"),
    DEVICE_ASSIGNED("success.telemetry.device_assigned", SuccessOutcome.CREATED,
            "Device attached to the shipment"),
    DEVICE_DETACHED("success.telemetry.device_detached", SuccessOutcome.UPDATED,
            "Device detached"),
    BATCH_INGESTED("success.telemetry.batch_ingested", SuccessOutcome.ACCEPTED, "Batch ingested"),
    SERIES_RETRIEVED("success.telemetry.series_retrieved", SuccessOutcome.RETRIEVED,
            "Temperature series retrieved");

    private final String messageKey;

    private final SuccessOutcome outcome;

    private final String message;

    TelemetrySuccessCode(String messageKey, SuccessOutcome outcome, String message) {
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
