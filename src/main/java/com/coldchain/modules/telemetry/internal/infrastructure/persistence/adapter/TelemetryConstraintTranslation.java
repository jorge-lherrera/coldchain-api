package com.coldchain.modules.telemetry.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.telemetry.internal.exception.TelemetryErrorCode;
import com.coldchain.shared.error.DomainException;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;

final class TelemetryConstraintTranslation {

    private static final Map<String, TelemetryErrorCode> BY_CONSTRAINT = Map.of(
            "UX_DEVICE_ASSIGNMENT_ACTIVE", TelemetryErrorCode.DEVICE_ALREADY_ASSIGNED,
            "UX_DEVICE_SERIAL_LIVE", TelemetryErrorCode.SERIAL_ALREADY_REGISTERED);

    private TelemetryConstraintTranslation() {
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
