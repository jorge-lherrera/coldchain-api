package com.coldchain.modules.shipment.api.dto;

import java.util.UUID;

public record OpenHandoffCommand(UUID shipmentId, UUID toOrganizationId) {
}
