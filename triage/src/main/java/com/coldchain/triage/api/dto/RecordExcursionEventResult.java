package com.coldchain.triage.api.dto;

import com.coldchain.triage.api.EventOutcome;
import java.util.UUID;

public record RecordExcursionEventResult(UUID eventId, UUID excursionId, EventOutcome outcome,
        boolean needsContext) {
}
