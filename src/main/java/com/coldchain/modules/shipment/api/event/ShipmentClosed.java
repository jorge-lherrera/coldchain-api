package com.coldchain.modules.shipment.api.event;

import com.coldchain.modules.shipment.api.ShipmentStatus;
import java.time.Instant;
import java.util.UUID;

public record ShipmentClosed(UUID shipmentId, UUID organizationId, ShipmentStatus status,
        Instant closedAt) {
}
