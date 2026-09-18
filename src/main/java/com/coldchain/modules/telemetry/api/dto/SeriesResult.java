package com.coldchain.modules.telemetry.api.dto;

import java.util.List;
import java.util.UUID;

public record SeriesResult(UUID shipmentId, List<SeriesPoint> points,
        List<ExcursionResult> excursions) {
}
