package com.coldchain.modules.telemetry.internal.domain.model;

import com.coldchain.modules.telemetry.api.BatchStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ReadingBatch {

    private final UUID id;

    private final UUID organizationId;

    private final UUID deviceId;

    private final String idempotencyKey;

    private final BatchStatus status;

    private final int receivedCount;

    private final int acceptedCount;

    private final int discardedCount;

    private final Instant receivedAt;

    private ReadingBatch(UUID id, UUID organizationId, UUID deviceId, String idempotencyKey,
            BatchStatus status, int receivedCount, int acceptedCount, int discardedCount,
            Instant receivedAt) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.deviceId = Objects.requireNonNull(deviceId);
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.status = Objects.requireNonNull(status);
        this.receivedCount = receivedCount;
        this.acceptedCount = acceptedCount;
        this.discardedCount = discardedCount;
        this.receivedAt = Objects.requireNonNull(receivedAt);
    }

    public static ReadingBatch createNew(UUID id, UUID organizationId, UUID deviceId,
            String idempotencyKey, int receivedCount, int acceptedCount, int discardedCount,
            Instant receivedAt) {
        return new ReadingBatch(id, organizationId, deviceId, idempotencyKey,
                statusOf(receivedCount, acceptedCount), receivedCount, acceptedCount, discardedCount,
                receivedAt);
    }

    public static ReadingBatch restore(UUID id, UUID organizationId, UUID deviceId,
            String idempotencyKey, BatchStatus status, int receivedCount, int acceptedCount,
            int discardedCount, Instant receivedAt) {
        return new ReadingBatch(id, organizationId, deviceId, idempotencyKey, status, receivedCount,
                acceptedCount, discardedCount, receivedAt);
    }

    private static BatchStatus statusOf(int receivedCount, int acceptedCount) {
        if (acceptedCount == 0) {
            return BatchStatus.REJECTED;
        }
        return acceptedCount == receivedCount ? BatchStatus.ACCEPTED : BatchStatus.PARTIAL;
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID deviceId() {
        return deviceId;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public BatchStatus status() {
        return status;
    }

    public int receivedCount() {
        return receivedCount;
    }

    public int acceptedCount() {
        return acceptedCount;
    }

    public int discardedCount() {
        return discardedCount;
    }

    public Instant receivedAt() {
        return receivedAt;
    }
}
