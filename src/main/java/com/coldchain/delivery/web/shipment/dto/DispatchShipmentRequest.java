package com.coldchain.delivery.web.shipment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DispatchShipmentRequest(@NotNull UUID deviceId) {
}
