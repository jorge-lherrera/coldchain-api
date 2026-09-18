package com.coldchain.modules.telemetry.api.dto;

import com.coldchain.modules.telemetry.api.DiscardReason;
import java.time.Instant;

public record DiscardedReading(Instant measuredAt, DiscardReason reason) {
}
