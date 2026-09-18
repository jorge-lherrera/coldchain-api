package com.coldchain.modules.shipment.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ShipmentLineCommand(UUID productId, BigDecimal quantity, String unit) {
}
