package com.coldchain.delivery.web.shipment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ShipmentLineResponse(UUID id, UUID productId, String productName, int profileVersion,
        BigDecimal quantity, String unit) {
}
