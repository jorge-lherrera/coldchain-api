package com.coldchain.modules.advisory.internal.domain.model;

import com.coldchain.shared.util.UuidV7;
import java.util.Objects;
import java.util.UUID;

public final class ExcursionAlert {

    private final UUID id;

    private final UUID organizationId;

    private final UUID shipmentId;

    private final UUID excursionId;

    private final Advice advice;

    private final long lockVersion;

    private ExcursionAlert(UUID id, UUID organizationId, UUID shipmentId, UUID excursionId, Advice advice,
            long lockVersion) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.shipmentId = Objects.requireNonNull(shipmentId);
        this.excursionId = Objects.requireNonNull(excursionId);
        this.advice = Objects.requireNonNull(advice);
        this.lockVersion = lockVersion;
    }

    public static ExcursionAlert createNew(UUID organizationId, UUID shipmentId, UUID excursionId,
            Advice advice) {
        return new ExcursionAlert(UuidV7.generate(), organizationId, shipmentId, excursionId, advice, 0L);
    }

    public static ExcursionAlert restore(UUID id, UUID organizationId, UUID shipmentId, UUID excursionId,
            Advice advice, long lockVersion) {
        return new ExcursionAlert(id, organizationId, shipmentId, excursionId, advice, lockVersion);
    }

    public boolean outdatedBy(Advice incoming) {
        return incoming.newerThan(advice);
    }

    public ExcursionAlert reconsideredWith(Advice incoming) {
        if (!outdatedBy(incoming)) {
            return this;
        }
        return new ExcursionAlert(id, organizationId, shipmentId, excursionId, incoming, lockVersion);
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID shipmentId() {
        return shipmentId;
    }

    public UUID excursionId() {
        return excursionId;
    }

    public Advice advice() {
        return advice;
    }

    public long lockVersion() {
        return lockVersion;
    }
}
