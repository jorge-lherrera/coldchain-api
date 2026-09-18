package com.coldchain.modules.shipment.api.event;

import java.time.Instant;
import java.util.UUID;

public sealed interface ShipmentEvent permits ShipmentDispatched, ShipmentClosed {

    UUID shipmentId();

    UUID organizationId();

    Instant occurredAt();
}
