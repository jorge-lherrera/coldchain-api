package com.coldchain.modules.telemetry.internal.domain.repository;

import com.coldchain.modules.telemetry.internal.domain.model.ReadingBatch;
import java.util.Optional;

public interface ReadingBatchRepository {

    ReadingBatch save(ReadingBatch batch);

    Optional<ReadingBatch> findByIdempotencyKey(String idempotencyKey);
}
