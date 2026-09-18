package com.coldchain.modules.identity.internal.infrastructure.persistence.entity;

import com.coldchain.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "API_CLIENT",
        indexes = @Index(name = "IX_API_CLIENT_ORGANIZATION_ID", columnList = "ORGANIZATION_ID"))
public class ApiClientJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ORGANIZATION_ID", nullable = false)
    private UUID organizationId;

    @Column(name = "CLIENT_ID", nullable = false)
    private String clientId;

    @Column(name = "SECRET_HASH", nullable = false)
    private String secretHash;

    @Column(name = "LABEL", nullable = false)
    private String label;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "LAST_USED_AT")
    private Instant lastUsedAt;

    protected ApiClientJpaEntity() {
    }

    public ApiClientJpaEntity(UUID id, UUID organizationId, String clientId, String secretHash, String label, String status, Instant lastUsedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.clientId = clientId;
        this.secretHash = secretHash;
        this.label = label;
        this.status = status;
        this.lastUsedAt = lastUsedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getSecretHash() {
        return secretHash;
    }

    public String getLabel() {
        return label;
    }

    public String getStatus() {
        return status;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }
}
