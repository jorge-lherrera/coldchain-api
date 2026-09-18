package com.coldchain.modules.shipment.api.dto;

import java.util.UUID;

public record DispatchShipmentCommand(UUID shipmentId, UUID deviceId) {
}
