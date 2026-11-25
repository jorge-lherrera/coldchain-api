package com.coldchain.modules.telemetry.api.dto;

import java.time.Instant;
import java.util.List;

public record ExcursionWindowResult(ExcursionResult excursion, Instant from, Instant to,
        List<SeriesPoint> readings) {
}
