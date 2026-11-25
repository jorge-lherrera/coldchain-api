package com.coldchain.triage.internal.domain.repository;

import com.coldchain.triage.internal.domain.model.ExcursionAlert;
import java.util.Optional;
import java.util.UUID;

public interface ExcursionAlertRepository {

    Optional<ExcursionAlert> findByExcursionId(UUID excursionId);

    ExcursionAlert save(ExcursionAlert alert);
}
