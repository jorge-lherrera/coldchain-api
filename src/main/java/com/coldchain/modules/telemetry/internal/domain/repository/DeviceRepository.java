package com.coldchain.modules.telemetry.internal.domain.repository;

import com.coldchain.modules.telemetry.internal.domain.model.Device;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository {

    Device save(Device device);

    Optional<Device> findById(UUID id);

    PagedResult<Device> findByOrganization(UUID organizationId, PageCriteria criteria);
}
