package com.coldchain.triage.internal.infrastructure.persistence.jpa;

import com.coldchain.triage.internal.infrastructure.persistence.entity.InboxEventJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxEventJpaRepository extends JpaRepository<InboxEventJpaEntity, UUID> {
}
