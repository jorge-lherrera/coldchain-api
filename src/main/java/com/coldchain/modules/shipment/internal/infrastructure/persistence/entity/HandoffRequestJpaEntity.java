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
@Table(name = "HANDOFF_REQUEST", indexes = {
        @Index(name = "IX_HANDOFF_REQUEST_SHIPMENT_ID", columnList = "SHIPMENT_ID"), 
        @Index(name = "IX_HANDOFF_REQUEST_TO_ORG_ID", columnList = "TO_ORG_ID")})
public class HandoffRequestJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "SHIPMENT_ID", nullable = false)
    private UUID shipmentId;

    @Column(name = "FROM_ORG_ID", nullable = false)
    private UUID fromOrganizationId;

    @Column(name = "TO_ORG_ID", nullable = false)
    private UUID toOrganizationId;

    @Column(name = "CODE_HASH", nullable = false)
    private String codeHash;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "EXPIRES_AT", nullable = false)
    private Instant expiresAt;

    @Column(name = "RESOLVED_AT")
    private Instant resolvedAt;

    protected HandoffRequestJpaEntity() {
    }

    public HandoffRequestJpaEntity(UUID id, UUID shipmentId, UUID fromOrganizationId, UUID toOrganizationId, String codeHash, String status, Instant expiresAt, Instant resolvedAt) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.fromOrganizationId = fromOrganizationId;
        this.toOrganizationId = toOrganizationId;
        this.codeHash = codeHash;
        this.status = status;
        this.expiresAt = expiresAt;
        this.resolvedAt = resolvedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public UUID getFromOrganizationId() {
        return fromOrganizationId;
    }

    public UUID getToOrganizationId() {
        return toOrganizationId;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public String getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
