package com.coldchain.delivery.web.shipment;

import com.coldchain.shared.response.SuccessCode;
import com.coldchain.shared.response.SuccessOutcome;

public enum ShipmentSuccessCode implements SuccessCode {

    SHIPMENT_CREATED("success.shipment.shipment_created", SuccessOutcome.CREATED, "Shipment created"),
    SHIPMENT_DISPATCHED("success.shipment.shipment_dispatched", SuccessOutcome.UPDATED,
            "Shipment dispatched"),
    SHIPMENT_ARRIVED("success.shipment.shipment_arrived", SuccessOutcome.UPDATED,
            "Shipment arrived at destination"),
    SHIPMENT_DELIVERED("success.shipment.shipment_delivered", SuccessOutcome.UPDATED,
            "Shipment delivered"),
    SHIPMENT_REJECTED("success.shipment.shipment_rejected", SuccessOutcome.UPDATED,
            "Shipment rejected"),
    SHIPMENT_CANCELLED("success.shipment.shipment_cancelled", SuccessOutcome.UPDATED,
            "Shipment cancelled"),
    SHIPMENT_RETRIEVED("success.shipment.shipment_retrieved", SuccessOutcome.RETRIEVED,
            "Shipment retrieved"),
    SHIPMENT_LISTED("success.shipment.shipment_listed", SuccessOutcome.RETRIEVED,
            "Shipments retrieved"),
    TIMELINE_RETRIEVED("success.shipment.timeline_retrieved", SuccessOutcome.RETRIEVED,
            "Custody timeline retrieved"),
    HANDOFF_OPENED("success.shipment.handoff_opened", SuccessOutcome.CREATED, "Handoff opened"),
    HANDOFF_ACCEPTED("success.shipment.handoff_accepted", SuccessOutcome.UPDATED, "Handoff accepted"),
    HANDOFF_REJECTED("success.shipment.handoff_rejected", SuccessOutcome.UPDATED, "Handoff rejected"),
    PARTICIPANT_ADDED("success.shipment.participant_added", SuccessOutcome.CREATED,
            "Participant added"),
    PARTICIPANT_REVOKED("success.shipment.participant_revoked", SuccessOutcome.DELETED,
            "Participant revoked");

    private final String messageKey;

    private final SuccessOutcome outcome;

    private final String message;

    ShipmentSuccessCode(String messageKey, SuccessOutcome outcome, String message) {
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
