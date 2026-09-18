package com.coldchain.modules.shipment.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ShipmentLineResult(UUID id, UUID productId, String productName, int profileVersion,
        BigDecimal quantity, String unit) {
}
