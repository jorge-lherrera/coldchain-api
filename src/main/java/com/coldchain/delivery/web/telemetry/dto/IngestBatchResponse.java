package com.coldchain.delivery.web.telemetry.dto;

import com.coldchain.modules.telemetry.api.BatchStatus;
import com.coldchain.modules.telemetry.api.dto.DiscardedReading;
import java.util.List;
import java.util.UUID;

public record IngestBatchResponse(UUID batchId, BatchStatus status, int receivedCount,
        int acceptedCount, int discardedCount, List<DiscardedReading> discarded) {
}
