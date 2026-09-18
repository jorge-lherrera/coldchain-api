package com.coldchain.modules.catalog.api.dto;

import java.util.UUID;

public record CreateProductCommand(UUID organizationId, String sku, String name, UUID storageProfileId) {
}
