package com.coldchain.modules.telemetry.api.event;

import java.time.Instant;
import java.util.UUID;

public sealed interface TelemetryEvent permits ExcursionOpened, ExcursionClosed {

    UUID excursionId();

    UUID organizationId();

    UUID shipmentId();

    Instant occurredAt();
}
