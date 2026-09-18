package com.coldchain.modules.shipment.api.dto;

import java.util.UUID;

public record AcceptHandoffCommand(UUID shipmentId, UUID acceptingOrganizationId, String code) {
}
