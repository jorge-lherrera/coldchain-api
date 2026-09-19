package com.coldchain.modules.shipment.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.shipment.internal.exception.ShipmentErrorCode;
import com.coldchain.shared.exception.DomainException;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;

final class ShipmentConstraintTranslation {

    private static final Map<String, ShipmentErrorCode> BY_CONSTRAINT = Map.of(
            "UX_SHIPMENT_REFERENCE", ShipmentErrorCode.REFERENCE_ALREADY_IN_USE,
            "UX_HANDOFF_REQUEST_PENDING", ShipmentErrorCode.HANDOFF_ALREADY_PENDING,
            "UX_SHIPMENT_PARTICIPANT_LIVE", ShipmentErrorCode.PARTICIPANT_ALREADY_ADDED);

    private ShipmentConstraintTranslation() {
    }

    static RuntimeException translate(DataIntegrityViolationException cause) {
        String message = cause.getMostSpecificCause().getMessage().toUpperCase(Locale.ROOT);
        return BY_CONSTRAINT.entrySet().stream()
                .filter(entry -> message.contains(entry.getKey()))
                .findFirst()
                .<RuntimeException>map(entry -> DomainException.of(entry.getValue()))
                .orElse(cause);
    }
}
