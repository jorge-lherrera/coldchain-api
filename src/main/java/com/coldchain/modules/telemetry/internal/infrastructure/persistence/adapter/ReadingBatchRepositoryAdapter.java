package com.coldchain.modules.telemetry.internal.infrastructure.persistence.adapter;

import com.coldchain.modules.telemetry.internal.domain.model.ReadingBatch;
import com.coldchain.modules.telemetry.internal.domain.repository.ReadingBatchRepository;
import com.coldchain.modules.telemetry.internal.infrastructure.persistence.jpa.ReadingBatchJpaRepository;
import com.coldchain.modules.telemetry.internal.infrastructure.persistence.mapper.TelemetryPersistenceMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ReadingBatchRepositoryAdapter implements ReadingBatchRepository {

    private final ReadingBatchJpaRepository batches;

    private final TelemetryPersistenceMapper mapper;

    public ReadingBatchRepositoryAdapter(ReadingBatchJpaRepository batches,
            TelemetryPersistenceMapper mapper) {
        this.batches = batches;
        this.mapper = mapper;
    }

    @Override
    public ReadingBatch save(ReadingBatch batch) {
        batches.saveAndFlush(mapper.toEntity(batch));
        return batch;
    }

    @Override
    public Optional<ReadingBatch> findByIdempotencyKey(String idempotencyKey) {
        return batches.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }
}
