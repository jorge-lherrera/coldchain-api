package com.coldchain.delivery.web.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank @Size(max = 60) String sku,
        @NotBlank @Size(max = 160) String name,
        @NotNull UUID storageProfileId) {
}
