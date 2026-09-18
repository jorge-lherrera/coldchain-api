package com.coldchain.modules.identity.internal.infrastructure.persistence.entity;

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
@Table(name = "REFRESH_TOKEN", indexes = {
        @Index(name = "IX_REFRESH_TOKEN_ORGANIZATION_ID", columnList = "ORGANIZATION_ID"),
        @Index(name = "IX_REFRESH_TOKEN_APP_USER_ID", columnList = "APP_USER_ID"),
        @Index(name = "IX_REFRESH_TOKEN_FAMILY_ID", columnList = "FAMILY_ID")})
public class RefreshTokenJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ORGANIZATION_ID", nullable = false)
    private UUID organizationId;

    @Column(name = "APP_USER_ID", nullable = false)
    private UUID appUserId;

    @Column(name = "FAMILY_ID", nullable = false)
    private UUID familyId;

    @Column(name = "TOKEN_HASH", nullable = false)
    private String tokenHash;

    @Column(name = "ISSUED_AT", nullable = false)
    private Instant issuedAt;

    @Column(name = "EXPIRES_AT", nullable = false)
    private Instant expiresAt;

    @Column(name = "USED_AT")
    private Instant usedAt;

    @Column(name = "REVOKED_AT")
    private Instant revokedAt;

    @Version
    @Column(name = "LOCK_VERSION", nullable = false)
    private long lockVersion;

    protected RefreshTokenJpaEntity() {
    }

    public RefreshTokenJpaEntity(UUID id, UUID organizationId, UUID appUserId, UUID familyId, String tokenHash, Instant issuedAt, Instant expiresAt, Instant usedAt, Instant revokedAt, long lockVersion) {
        this.id = id;
        this.organizationId = organizationId;
        this.appUserId = appUserId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.revokedAt = revokedAt;
        this.lockVersion = lockVersion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getAppUserId() {
        return appUserId;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}
