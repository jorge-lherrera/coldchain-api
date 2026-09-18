package com.coldchain.modules.telemetry.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.telemetry.internal.domain.model.SensorDevice;
import com.coldchain.modules.telemetry.internal.domain.repository.SensorDeviceRepository;
import com.coldchain.modules.telemetry.internal.infrastructure.persistence.jpa.SensorDeviceJpaRepository;
import com.coldchain.modules.telemetry.internal.infrastructure.persistence.mapper.TelemetryPersistenceMapper;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import com.coldchain.shared.paging.SpringDataPaging;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class SensorDeviceRepositoryAdapter implements SensorDeviceRepository {

    private final SensorDeviceJpaRepository devices;

    private final TelemetryPersistenceMapper mapper;

    public SensorDeviceRepositoryAdapter(SensorDeviceJpaRepository devices,
            TelemetryPersistenceMapper mapper) {
        this.devices = devices;
        this.mapper = mapper;
    }

    @Override
    public SensorDevice save(SensorDevice device) {
        try {
            devices.saveAndFlush(mapper.toEntity(device));
        } catch (DataIntegrityViolationException cause) {
            throw TelemetryConstraintTranslation.translate(cause);
        }
        return device;
    }

    @Override
    public Optional<SensorDevice> findById(UUID id) {
        return devices.findById(id).map(mapper::toDomain);
    }

    @Override
    public PagedResult<SensorDevice> findByOrganization(UUID organizationId, PageCriteria criteria) {
        return SpringDataPaging.toPagedResult(
                devices.findByOrganizationIdAndDeletedAtIsNull(organizationId,
                        SpringDataPaging.toPageable(criteria)).map(mapper::toDomain),
                criteria);
    }
}
