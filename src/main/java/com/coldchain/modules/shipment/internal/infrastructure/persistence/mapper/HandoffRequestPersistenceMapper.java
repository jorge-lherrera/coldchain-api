package com.coldchain.modules.shipment.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.shipment.api.HandoffStatus;
import com.coldchain.modules.shipment.internal.domain.model.HandoffRequest;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.entity.HandoffRequestJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class HandoffRequestPersistenceMapper {

    public HandoffRequestJpaEntity toEntity(HandoffRequest request) {
        return new HandoffRequestJpaEntity(request.id(), request.shipmentId(),
                request.fromOrganizationId(), request.toOrganizationId(), request.codeHash(),
                request.status().name(), request.expiresAt(), request.resolvedAt(), request.lockVersion());
    }

    public HandoffRequest toDomain(HandoffRequestJpaEntity entity) {
        return HandoffRequest.restore(entity.getId(), entity.getShipmentId(),
                entity.getFromOrganizationId(), entity.getToOrganizationId(), entity.getCodeHash(),
                HandoffStatus.valueOf(entity.getStatus()), entity.getExpiresAt(),
                entity.getResolvedAt(), entity.getLockVersion());
    }
}
