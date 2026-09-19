package com.coldchain.modules.identity.internal.infrastructure.persistence.entity;

import com.coldchain.shared.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "APP_USER",
        indexes = @Index(name = "IX_APP_USER_ORGANIZATION_ID", columnList = "ORGANIZATION_ID"))
public class AppUserJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ORGANIZATION_ID", nullable = false)
    private UUID organizationId;

    @Column(name = "EMAIL", nullable = false)
    private String email;

    @Column(name = "PASSWORD_HASH")
    private String passwordHash;

    @Column(name = "FULL_NAME", nullable = false)
    private String fullName;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "ACTIVATION_TOKEN_HASH")
    private String activationTokenHash;

    @Column(name = "ACTIVATION_EXPIRES_AT")
    private Instant activationExpiresAt;

    @Column(name = "LAST_LOGIN_AT")
    private Instant lastLoginAt;

    @Version
    @Column(name = "LOCK_VERSION", nullable = false)
    private long lockVersion;

    protected AppUserJpaEntity() {
    }

    public AppUserJpaEntity(UUID id, UUID organizationId, String email, String passwordHash, String fullName, String status, String activationTokenHash, Instant activationExpiresAt, Instant lastLoginAt, long lockVersion) {
        this.id = id;
        this.organizationId = organizationId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.status = status;
        this.activationTokenHash = activationTokenHash;
        this.activationExpiresAt = activationExpiresAt;
        this.lastLoginAt = lastLoginAt;
        this.lockVersion = lockVersion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getStatus() {
        return status;
    }

    public String getActivationTokenHash() {
        return activationTokenHash;
    }

    public Instant getActivationExpiresAt() {
        return activationExpiresAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}
