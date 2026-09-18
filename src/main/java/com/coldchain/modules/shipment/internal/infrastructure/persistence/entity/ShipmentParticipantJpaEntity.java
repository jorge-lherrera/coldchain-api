package com.coldchain.modules.shipment.internal.infrastructure.persistence.entity;

import com.coldchain.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "SHIPMENT_PARTICIPANT", indexes = {
        @Index(name = "IX_SHIPMENT_PARTICIPANT_SHIPMENT_ID", columnList = "SHIPMENT_ID"), 
        @Index(name = "IX_SHIPMENT_PARTICIPANT_ORGANIZATION_ID", columnList = "ORGANIZATION_ID")})
public class ShipmentParticipantJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "SHIPMENT_ID", nullable = false)
    private UUID shipmentId;

    @Column(name = "ORGANIZATION_ID", nullable = false)
    private UUID organizationId;

    @Column(name = "PARTICIPATION", nullable = false)
    private String participation;

    @Column(name = "REVOKED_AT")
    private Instant revokedAt;

    @Version
    @Column(name = "LOCK_VERSION", nullable = false)
    private long lockVersion;

    protected ShipmentParticipantJpaEntity() {
    }

    public ShipmentParticipantJpaEntity(UUID id, UUID shipmentId, UUID organizationId, String participation, Instant revokedAt, long lockVersion) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.organizationId = organizationId;
        this.participation = participation;
        this.revokedAt = revokedAt;
        this.lockVersion = lockVersion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getParticipation() {
        return participation;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}
