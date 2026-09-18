package com.coldchain.modules.shipment.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.shipment.api.CustodyEventKind;
import com.coldchain.modules.shipment.internal.domain.model.CustodyEvent;
import com.coldchain.modules.shipment.internal.infrastructure.persistence.entity.CustodyEventJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CustodyEventPersistenceMapper {

    public CustodyEventJpaEntity toEntity(CustodyEvent event) {
        return new CustodyEventJpaEntity(event.id(), event.shipmentId(), event.sequenceNumber(),
                event.kind().name(), event.fromOrganizationId(), event.toOrganizationId(),
                event.siteId(), event.actorId(), event.occurredAt(), event.recordedAt(),
                event.previousHash(), event.hash());
    }

    public CustodyEvent toDomain(CustodyEventJpaEntity entity) {
        return CustodyEvent.restore(entity.getId(), entity.getShipmentId(), entity.getSequenceNumber(),
                CustodyEventKind.valueOf(entity.getKind()), entity.getFromOrganizationId(),
                entity.getToOrganizationId(), entity.getSiteId(), entity.getActorId(),
                entity.getOccurredAt(), entity.getRecordedAt(), entity.getPreviousHash(),
                entity.getHash());
    }
}
