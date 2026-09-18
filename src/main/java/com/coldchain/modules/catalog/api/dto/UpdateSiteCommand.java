package com.coldchain.modules.catalog.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateSiteCommand(UUID siteId, String name, BigDecimal latitude, BigDecimal longitude,
        String timeZone) {
}
