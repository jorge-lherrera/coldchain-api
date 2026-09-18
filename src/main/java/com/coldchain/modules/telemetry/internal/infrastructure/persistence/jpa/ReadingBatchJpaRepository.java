package com.coldchain.modules.telemetry.internal.infrastructure.persistence.jpa;

import com.coldchain.modules.telemetry.internal.infrastructure.persistence.entity.ReadingBatchJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingBatchJpaRepository extends JpaRepository<ReadingBatchJpaEntity, UUID> {

    Optional<ReadingBatchJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
