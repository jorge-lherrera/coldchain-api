package com.coldchain.modules.telemetry.internal.domain.repository;

import com.coldchain.modules.telemetry.internal.domain.model.SensorDevice;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.util.Optional;
import java.util.UUID;

public interface SensorDeviceRepository {

    SensorDevice save(SensorDevice device);

    Optional<SensorDevice> findById(UUID id);

    PagedResult<SensorDevice> findByOrganization(UUID organizationId, PageCriteria criteria);
}
