package com.coldchain.delivery.web.shipment.dto;

import com.coldchain.modules.shipment.api.dto.ChainVerdict;
import java.util.List;
import java.util.UUID;

public record ShipmentTimelineResponse(UUID shipmentId, List<CustodyEventResponse> events,
        ChainVerdict verdict) {
}
