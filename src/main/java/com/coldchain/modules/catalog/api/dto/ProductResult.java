package com.coldchain.modules.catalog.api.dto;

import java.util.UUID;

public record ProductResult(UUID id, UUID organizationId, String sku, String name,
        UUID storageProfileId) {
}
