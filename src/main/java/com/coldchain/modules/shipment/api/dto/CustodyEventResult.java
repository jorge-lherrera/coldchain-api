package com.coldchain.modules.shipment.api.dto;

import com.coldchain.modules.shipment.api.CustodyEventKind;
import java.time.Instant;
import java.util.UUID;

public record CustodyEventResult(
        UUID id,
        int sequenceNumber,
        CustodyEventKind kind,
        UUID fromOrganizationId,
        UUID toOrganizationId,
        UUID siteId,
        UUID actorId,
        Instant occurredAt,
        Instant recordedAt,
        String previousHash,
        String hash) {
}
