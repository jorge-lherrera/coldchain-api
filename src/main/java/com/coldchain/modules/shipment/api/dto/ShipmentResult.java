package com.coldchain.modules.shipment.api.dto;

import com.coldchain.modules.shipment.api.ShipmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShipmentResult(
        UUID id,
        UUID organizationId,
        String reference,
        ShipmentStatus status,
        UUID originSiteId,
        UUID destinationSiteId,
        UUID consigneeOrganizationId,
        UUID currentCustodianOrganizationId,
        UUID deviceId,
        ShipmentThresholds thresholds,
        boolean openExcursion,
        Instant dispatchedAt,
        Instant closedAt,
        List<ShipmentLineResult> lines) {
}
