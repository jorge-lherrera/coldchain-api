package com.coldchain.delivery.web.shipment.dto;

import com.coldchain.modules.shipment.api.CustodyEventKind;
import java.time.Instant;
import java.util.UUID;

public record CustodyEventResponse(
        int sequenceNumber,
        CustodyEventKind kind,
        UUID fromOrganizationId,
        UUID toOrganizationId,
        UUID siteId,
        Instant occurredAt,
        Instant recordedAt,
        String hash) {
}
