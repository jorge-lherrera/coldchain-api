package com.coldchain.delivery.web.catalog.dto;

import com.coldchain.modules.catalog.api.ProfileStatus;
import java.util.UUID;

public record StorageProfileResponse(
        UUID id,
        String code,
        String name,
        int version,
        ProfileStatus status,
        ThresholdsPayload thresholds) {
}
