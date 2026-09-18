package com.coldchain.modules.identity.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.identity.internal.infrastructure.persistence.entity.AuditEntryJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEntryJpaRepository extends JpaRepository<AuditEntryJpaEntity, UUID> {
}
