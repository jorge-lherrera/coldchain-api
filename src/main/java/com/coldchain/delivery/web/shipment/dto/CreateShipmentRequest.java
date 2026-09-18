package com.coldchain.delivery.web.shipment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateShipmentRequest(
        @NotBlank @Size(max = 40) String reference,
        @NotNull UUID originSiteId,
        @NotNull UUID destinationSiteId,
        @NotNull UUID consigneeOrganizationId,
        @NotEmpty @Valid List<ShipmentLinePayload> lines) {
}
