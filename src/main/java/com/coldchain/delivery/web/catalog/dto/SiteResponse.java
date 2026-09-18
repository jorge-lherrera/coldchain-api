package com.coldchain.delivery.web.catalog.dto;

import com.coldchain.modules.catalog.api.SiteKind;
import java.math.BigDecimal;
import java.util.UUID;

public record SiteResponse(UUID id, String code, String name, SiteKind kind, BigDecimal latitude,
        BigDecimal longitude, String timeZone) {
}
