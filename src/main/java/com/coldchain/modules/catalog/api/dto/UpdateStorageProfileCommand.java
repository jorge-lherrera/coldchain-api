package com.coldchain.modules.catalog.api.dto;

import java.util.UUID;

public record UpdateStorageProfileCommand(UUID profileId, String name, StorageThresholds thresholds) {
}
