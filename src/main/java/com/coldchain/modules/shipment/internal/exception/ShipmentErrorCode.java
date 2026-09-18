package com.coldchain.modules.shipment.internal.exception;

import com.coldchain.shared.error.ErrorCategory;
import com.coldchain.shared.error.ErrorCode;

public enum ShipmentErrorCode implements ErrorCode {

    SHIPMENT_NOT_FOUND("error.shipment.shipment_not_found", ErrorCategory.NOT_FOUND,
            "The shipment does not exist or you do not take part in it"),
    REFERENCE_ALREADY_IN_USE("error.shipment.reference_already_in_use", ErrorCategory.CONFLICT,
            "Another shipment already uses that reference"),
    ILLEGAL_TRANSITION("error.shipment.illegal_transition", ErrorCategory.BUSINESS_RULE,
            "The shipment cannot move to that status from where it is"),
    NO_LINES("error.shipment.no_lines", ErrorCategory.BUSINESS_RULE,
            "A shipment with no lines carries nothing"),
    NO_DEVICE("error.shipment.no_device", ErrorCategory.BUSINESS_RULE,
            "A shipment with no device cannot be monitored"),
    NOT_THE_CUSTODIAN("error.shipment.not_the_custodian", ErrorCategory.AUTHORIZATION,
            "Only the organization holding custody can do that"),
    NOT_THE_SHIPPER("error.shipment.not_the_shipper", ErrorCategory.AUTHORIZATION,
            "Only the shipper can change who takes part in a shipment"),
    HANDOFF_ALREADY_PENDING("error.shipment.handoff_already_pending", ErrorCategory.CONFLICT,
            "That shipment already has a handoff waiting to be accepted"),
    HANDOFF_NOT_FOUND("error.shipment.handoff_not_found", ErrorCategory.NOT_FOUND,
            "There is no handoff waiting on that shipment"),
    HANDOFF_CODE_INVALID("error.shipment.handoff_code_invalid", ErrorCategory.BUSINESS_RULE,
            "That handoff code is not the one that was issued"),
    HANDOFF_EXPIRED("error.shipment.handoff_expired", ErrorCategory.BUSINESS_RULE,
            "The handoff code expired"),
    HANDOFF_NOT_YOURS("error.shipment.handoff_not_yours", ErrorCategory.AUTHORIZATION,
            "The handoff was opened for another organization"),
    PARTICIPANT_ALREADY_ADDED("error.shipment.participant_already_added", ErrorCategory.CONFLICT,
            "That organization already takes part in the shipment"),
    PARTICIPANT_NOT_FOUND("error.shipment.participant_not_found", ErrorCategory.NOT_FOUND,
            "That organization does not take part in the shipment"),
    PROFILE_NOT_FROZEN("error.shipment.profile_not_frozen", ErrorCategory.BUSINESS_RULE,
            "The storage profile of a line could not be read, so nothing can be frozen");

    private final String messageKey;

    private final ErrorCategory category;

    private final String title;

    ShipmentErrorCode(String messageKey, ErrorCategory category, String title) {
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
