package com.coldchain.modules.catalog.api.dto;

import java.util.UUID;

public record UpdateProductCommand(UUID productId, String name, UUID storageProfileId) {
}
