package com.coldchain.modules.identity.internal.domain.model;

import com.coldchain.shared.util.UuidV7;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class RefreshToken {

    private final UUID id;

    private final UUID organizationId;

    private final UUID appUserId;

    private final UUID familyId;

    private final String tokenHash;

    private final Instant issuedAt;

    private final Instant expiresAt;

    private final Instant usedAt;

    private final Instant revokedAt;

    private final long lockVersion;

    private RefreshToken(UUID id, UUID organizationId, UUID appUserId, UUID familyId, String tokenHash,
            Instant issuedAt, Instant expiresAt, Instant usedAt, Instant revokedAt, long lockVersion) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.appUserId = Objects.requireNonNull(appUserId);
        this.familyId = Objects.requireNonNull(familyId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.issuedAt = Objects.requireNonNull(issuedAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.usedAt = usedAt;
        this.revokedAt = revokedAt;
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("A refresh token expires after it is issued");
        }
        this.lockVersion = lockVersion;
    }

    public static RefreshToken openFamily(UUID organizationId, UUID appUserId, String tokenHash,
            Instant issuedAt, Duration lifetime) {
        UUID id = UuidV7.generate();
        return new RefreshToken(id, organizationId, appUserId, id, tokenHash, issuedAt,
                issuedAt.plus(lifetime), null, null, 0);
    }

    public static RefreshToken restore(UUID id, UUID organizationId, UUID appUserId, UUID familyId,
            String tokenHash, Instant issuedAt, Instant expiresAt, Instant usedAt, Instant revokedAt, long lockVersion) {
        return new RefreshToken(id, organizationId, appUserId, familyId, tokenHash, issuedAt, expiresAt, usedAt,
                revokedAt, lockVersion);
    }

    public RefreshToken rotateInto(String nextTokenHash, Instant issuedAt, Duration lifetime) {
        return new RefreshToken(UuidV7.generate(), organizationId, appUserId, familyId, nextTokenHash, issuedAt,
                issuedAt.plus(lifetime), null, null, lockVersion);
    }

    public RefreshToken markUsed(Instant when) {
        return new RefreshToken(id, organizationId, appUserId, familyId, tokenHash, issuedAt, expiresAt,
                Objects.requireNonNull(when), revokedAt, lockVersion);
    }

    public RefreshToken revoke(Instant when) {
        return new RefreshToken(id, organizationId, appUserId, familyId, tokenHash, issuedAt, expiresAt, usedAt,
                Objects.requireNonNull(when), lockVersion);
    }

    public boolean spent() {
        return usedAt != null;
    }

    public boolean revoked() {
        return revokedAt != null;
    }

    public boolean expiredAt(Instant when) {
        return !expiresAt.isAfter(when);
    }

    public boolean usableAt(Instant when) {
        return !spent() && !revoked() && !expiredAt(when);
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public UUID appUserId() {
        return appUserId;
    }

    public UUID familyId() {
        return familyId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant usedAt() {
        return usedAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    public long lockVersion() {
        return lockVersion;
    }
}
