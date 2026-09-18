package com.coldchain.delivery.web.shipment.dto;

import com.coldchain.modules.shipment.api.ShipmentStatus;
import com.coldchain.modules.shipment.api.dto.ShipmentThresholds;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        String reference,
        ShipmentStatus status,
        UUID originSiteId,
        UUID destinationSiteId,
        UUID consigneeOrganizationId,
        UUID currentCustodianOrganizationId,
        UUID deviceId,
        ShipmentThresholds thresholds,
        boolean hasOpenExcursion,
        Instant dispatchedAt,
        Instant closedAt,
        List<ShipmentLineResponse> lines) {
}
