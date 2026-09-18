package com.coldchain.modules.catalog.api.dto;

import com.coldchain.modules.catalog.api.SiteKind;
import java.math.BigDecimal;
import java.util.UUID;

public record SiteResult(UUID id, UUID organizationId, String code, String name, SiteKind kind,
        BigDecimal latitude, BigDecimal longitude, String timeZone) {
}
