package com.coldchain.modules.identity.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.identity.api.ActorType;
import com.coldchain.modules.identity.internal.domain.model.AuditEntry;
import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.AuditEntryJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AuditEntryPersistenceMapper {

    public AuditEntryJpaEntity toEntity(AuditEntry entry) {
        return new AuditEntryJpaEntity(entry.id(), entry.organizationId(), entry.actorType().name(),
                entry.actorId(), entry.action(), entry.resourceType(), entry.resourceId(), entry.payload(),
                entry.occurredAt());
    }

    public AuditEntry toDomain(AuditEntryJpaEntity entity) {
        return AuditEntry.record(entity.getOrganizationId(), ActorType.valueOf(entity.getActorType()),
                entity.getActorId(), entity.getAction(), entity.getResourceType(), entity.getResourceId(),
                entity.getPayload(), entity.getOccurredAt());
    }
}
