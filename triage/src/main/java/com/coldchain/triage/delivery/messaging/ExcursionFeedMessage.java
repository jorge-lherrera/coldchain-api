package com.coldchain.triage.delivery.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExcursionFeedMessage(UUID eventId, String type, Instant occurredAt, UUID shipmentId,
        UUID organizationId, UUID excursionId, String kind, BigDecimal peakCelsius, Long durationMinutes) {
}
