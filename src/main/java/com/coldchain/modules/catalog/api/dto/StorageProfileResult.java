package com.coldchain.modules.catalog.api.dto;

import com.coldchain.modules.catalog.api.ProfileStatus;
import java.util.UUID;

public record StorageProfileResult(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        int version,
        ProfileStatus status,
        ThresholdsView thresholds) {
}
