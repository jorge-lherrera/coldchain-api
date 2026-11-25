package com.coldchain.triage.internal.domain.service;

import com.coldchain.triage.internal.domain.model.ExcursionContext;
import java.util.Optional;
import java.util.UUID;

public interface ExcursionContextSource {

    Optional<ExcursionContext> contextOf(UUID shipmentId, UUID excursionId);
}
