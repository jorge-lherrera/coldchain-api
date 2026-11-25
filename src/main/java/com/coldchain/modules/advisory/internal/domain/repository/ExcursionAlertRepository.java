package com.coldchain.modules.advisory.internal.domain.repository;

import com.coldchain.modules.advisory.internal.domain.model.ExcursionAlert;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExcursionAlertRepository {

    ExcursionAlert save(ExcursionAlert alert);

    Optional<ExcursionAlert> findByExcursionId(UUID excursionId);

    List<ExcursionAlert> findOfShipment(UUID shipmentId);
}
