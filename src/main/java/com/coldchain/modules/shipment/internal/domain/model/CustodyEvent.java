package com.coldchain.modules.shipment.internal.domain.model;

import com.coldchain.modules.shipment.api.CustodyEventKind;
import com.coldchain.shared.util.UuidV7;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class CustodyEvent {

    public static final String GENESIS_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";

    private final UUID id;

    private final UUID shipmentId;

    private final int sequenceNumber;

    private final CustodyEventKind kind;

    private final UUID fromOrganizationId;

    private final UUID toOrganizationId;

    private final UUID siteId;

    private final UUID actorId;

    private final Instant occurredAt;

    private final Instant recordedAt;

    private final String previousHash;

    private final String hash;

    private CustodyEvent(UUID id, UUID shipmentId, int sequenceNumber, CustodyEventKind kind,
            UUID fromOrganizationId, UUID toOrganizationId, UUID siteId, UUID actorId,
            Instant occurredAt, Instant recordedAt, String previousHash, String hash) {
        this.id = Objects.requireNonNull(id);
        this.shipmentId = Objects.requireNonNull(shipmentId);
        this.sequenceNumber = sequenceNumber;
        this.kind = Objects.requireNonNull(kind);
        this.fromOrganizationId = fromOrganizationId;
        this.toOrganizationId = toOrganizationId;
        this.siteId = siteId;
        this.actorId = Objects.requireNonNull(actorId);
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.recordedAt = Objects.requireNonNull(recordedAt);
        this.previousHash = Objects.requireNonNull(previousHash);
        this.hash = Objects.requireNonNull(hash);
    }

    public static CustodyEvent createNew(UUID shipmentId, int sequenceNumber, CustodyEventKind kind,
            UUID fromOrganizationId, UUID toOrganizationId, UUID siteId, UUID actorId,
            Instant occurredAt, Instant recordedAt, String previousHash, String hash) {
        return new CustodyEvent(UuidV7.generate(), shipmentId, sequenceNumber, kind, fromOrganizationId,
                toOrganizationId, siteId, actorId, occurredAt, recordedAt, previousHash, hash);
    }

    public static CustodyEvent restore(UUID id, UUID shipmentId, int sequenceNumber,
            CustodyEventKind kind, UUID fromOrganizationId, UUID toOrganizationId, UUID siteId,
            UUID actorId, Instant occurredAt, Instant recordedAt, String previousHash, String hash) {
        return new CustodyEvent(id, shipmentId, sequenceNumber, kind, fromOrganizationId,
                toOrganizationId, siteId, actorId, occurredAt, recordedAt, previousHash, hash);
    }

    public UUID id() {
        return id;
    }

    public UUID shipmentId() {
        return shipmentId;
    }

    public int sequenceNumber() {
        return sequenceNumber;
    }

    public CustodyEventKind kind() {
        return kind;
    }

    public UUID fromOrganizationId() {
        return fromOrganizationId;
    }

    public UUID toOrganizationId() {
        return toOrganizationId;
    }

    public UUID siteId() {
        return siteId;
    }

    public UUID actorId() {
        return actorId;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public Instant recordedAt() {
        return recordedAt;
    }

    public String previousHash() {
        return previousHash;
    }

    public String hash() {
        return hash;
    }
}
