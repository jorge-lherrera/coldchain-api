package com.coldchain.modules.shipment.api.event;

import com.coldchain.modules.shipment.api.dto.ShipmentThresholds;
import java.time.Instant;
import java.util.UUID;

public record ShipmentDispatched(UUID shipmentId, UUID organizationId, UUID deviceId,
        ShipmentThresholds thresholds, Instant dispatchedAt) implements ShipmentEvent {

    @Override
    public Instant occurredAt() {
        return dispatchedAt;
    }
}
