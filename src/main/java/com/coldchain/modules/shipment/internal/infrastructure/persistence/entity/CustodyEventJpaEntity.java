package com.coldchain.modules.shipment.internal.infrastructure.persistence.entity;

import com.coldchain.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "CUSTODY_EVENT", indexes = {
        @Index(name = "IX_CUSTODY_EVENT_SHIPMENT_ID", columnList = "SHIPMENT_ID")})
public class CustodyEventJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "SHIPMENT_ID", nullable = false)
    private UUID shipmentId;

    @Column(name = "SEQUENCE_NUMBER", nullable = false)
    private int sequenceNumber;

    @Column(name = "KIND", nullable = false)
    private String kind;

    @Column(name = "FROM_ORGANIZATION_ID")
    private UUID fromOrganizationId;

    @Column(name = "TO_ORGANIZATION_ID")
    private UUID toOrganizationId;

    @Column(name = "SITE_ID")
    private UUID siteId;

    @Column(name = "ACTOR_ID", nullable = false)
    private UUID actorId;

    @Column(name = "OCCURRED_AT", nullable = false)
    private Instant occurredAt;

    @Column(name = "RECORDED_AT", nullable = false)
    private Instant recordedAt;

    @Column(name = "PREVIOUS_HASH", nullable = false)
    private String previousHash;

    @Column(name = "HASH", nullable = false)
    private String hash;

    protected CustodyEventJpaEntity() {
    }

    public CustodyEventJpaEntity(UUID id, UUID shipmentId, int sequenceNumber, String kind, UUID fromOrganizationId, UUID toOrganizationId, UUID siteId, UUID actorId, Instant occurredAt, Instant recordedAt, String previousHash, String hash) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.sequenceNumber = sequenceNumber;
        this.kind = kind;
        this.fromOrganizationId = fromOrganizationId;
        this.toOrganizationId = toOrganizationId;
        this.siteId = siteId;
        this.actorId = actorId;
        this.occurredAt = occurredAt;
        this.recordedAt = recordedAt;
        this.previousHash = previousHash;
        this.hash = hash;
    }

    public UUID getId() {
        return id;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getKind() {
        return kind;
    }

    public UUID getFromOrganizationId() {
        return fromOrganizationId;
    }

    public UUID getToOrganizationId() {
        return toOrganizationId;
    }

    public UUID getSiteId() {
        return siteId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getHash() {
        return hash;
    }
}
