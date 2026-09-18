package com.coldchain.delivery.web.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStorageProfileRequest(
        @NotBlank @Size(max = 160) String name,
        @NotNull @Valid ThresholdsPayload thresholds) {
}
