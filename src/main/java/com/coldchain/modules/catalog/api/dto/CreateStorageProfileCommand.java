package com.coldchain.modules.catalog.api.dto;

import java.util.UUID;

public record CreateStorageProfileCommand(UUID organizationId, String code, String name,
        ThresholdsView thresholds) {
}
