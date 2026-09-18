package com.coldchain.modules.shipment.api.dto;

import java.util.List;
import java.util.UUID;

public record CreateShipmentCommand(UUID organizationId, String reference, UUID originSiteId,
        UUID destinationSiteId, UUID consigneeOrganizationId, List<ShipmentLineCommand> lines) {
}
