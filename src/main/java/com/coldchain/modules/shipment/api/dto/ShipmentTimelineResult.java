package com.coldchain.modules.shipment.api.dto;

import java.util.List;
import java.util.UUID;

public record ShipmentTimelineResult(UUID shipmentId, List<CustodyEventResult> events,
        ChainVerdict verdict) {
}
