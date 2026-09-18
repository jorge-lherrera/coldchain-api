package com.coldchain.modules.telemetry.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.telemetry.internal.domain.model.Device;
import com.coldchain.modules.telemetry.internal.domain.repository.DeviceRepository;
import com.coldchain.modules.telemetry.internal.infrastructure.persistence.jpa.DeviceJpaRepository;
import com.coldchain.modules.telemetry.internal.infrastructure.persistence.mapper.TelemetryPersistenceMapper;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import com.coldchain.shared.paging.SpringDataPaging;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceRepositoryAdapter implements DeviceRepository {

    private final DeviceJpaRepository devices;

    private final TelemetryPersistenceMapper mapper;

    public DeviceRepositoryAdapter(DeviceJpaRepository devices,
            TelemetryPersistenceMapper mapper) {
        this.devices = devices;
        this.mapper = mapper;
    }

    @Override
    public Device save(Device device) {
        try {
            devices.saveAndFlush(mapper.toEntity(device));
        } catch (DataIntegrityViolationException cause) {
            throw TelemetryConstraintTranslation.translate(cause);
        }
        return device;
    }

    @Override
    public Optional<Device> findById(UUID id) {
        return devices.findById(id).map(mapper::toDomain);
    }

    @Override
    public PagedResult<Device> findByOrganization(UUID organizationId, PageCriteria criteria) {
        return SpringDataPaging.toPagedResult(
                devices.findByOrganizationIdAndDeletedAtIsNull(organizationId,
                        SpringDataPaging.toPageable(criteria)).map(mapper::toDomain),
                criteria);
    }
}
