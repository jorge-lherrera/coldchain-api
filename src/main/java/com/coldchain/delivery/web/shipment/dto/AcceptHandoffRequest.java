package com.coldchain.delivery.web.shipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptHandoffRequest(@NotBlank @Size(min = 4, max = 16) String code) {
}
