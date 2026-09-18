package com.coldchain.delivery.web.catalog.dto;

import java.util.UUID;

public record ProductResponse(UUID id, String sku, String name, UUID storageProfileId) {
}
