package com.coldchain.modules.telemetry.api.dto;

import com.coldchain.modules.telemetry.api.BatchStatus;
import java.util.List;
import java.util.UUID;

public record IngestBatchResult(UUID batchId, BatchStatus status, int receivedCount,
        int acceptedCount, int discardedCount, List<DiscardedReading> discarded) {
}
