package com.coldchain.modules.shipment.internal.domain.model;

import com.coldchain.modules.shipment.api.Participation;
import com.coldchain.shared.util.UuidV7;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ShipmentParticipant {

    private final UUID id;

    private final UUID shipmentId;

    private final UUID organizationId;

    private final Participation participation;

    private final Instant revokedAt;

    private final long lockVersion;

    private ShipmentParticipant(UUID id, UUID shipmentId, UUID organizationId,
            Participation participation, Instant revokedAt, long lockVersion) {
        this.id = Objects.requireNonNull(id);
        this.shipmentId = Objects.requireNonNull(shipmentId);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.participation = Objects.requireNonNull(participation);
        this.revokedAt = revokedAt;
        this.lockVersion = lockVersion;
    }

    public static ShipmentParticipant createNew(UUID shipmentId, UUID organizationId,
            Participation participation) {
        return new ShipmentParticipant(UuidV7.generate(), shipmentId, organizationId, participation,
                null, 0);
    }

    public static ShipmentParticipant restore(UUID id, UUID shipmentId, UUID organizationId,
            Participation participation, Instant revokedAt, long lockVersion) {
        return new ShipmentParticipant(id, shipmentId, organizationId, participation, revokedAt, lockVersion);
    }

    public ShipmentParticipant revoke(Instant when) {
        return new ShipmentParticipant(id, shipmentId, organizationId, participation, when, lockVersion);
    }

    public boolean live() {
        return revokedAt == null;
    }

    public UUID id() {
        return id;
    }

    public UUID shipmentId() {
        return shipmentId;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public Participation participation() {
        return participation;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    public long lockVersion() {
        return lockVersion;
    }
}
