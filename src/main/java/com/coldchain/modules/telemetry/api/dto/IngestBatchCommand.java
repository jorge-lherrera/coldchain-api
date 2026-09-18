package com.coldchain.modules.telemetry.api.dto;

import java.util.List;
import java.util.UUID;

public record IngestBatchCommand(UUID organizationId, UUID deviceId, String idempotencyKey,
        List<ReadingCommand> readings) {
}
