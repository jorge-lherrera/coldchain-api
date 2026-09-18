package com.coldchain.modules.identity.internal.domain.model;

import com.coldchain.modules.identity.api.UserStatus;
import com.coldchain.shared.identifier.UuidV7;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class AppUser {

    private final UUID id;

    private final UUID organizationId;

    private final String email;

    private final String passwordHash;

    private final String fullName;

    private final UserStatus status;

    private final String activationTokenHash;

    private final Instant activationExpiresAt;

    private final Instant lastLoginAt;

    private final long lockVersion;

    private AppUser(UUID id, UUID organizationId, String email, String passwordHash, String fullName,
            UserStatus status, String activationTokenHash, Instant activationExpiresAt, Instant lastLoginAt, long lockVersion) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.email = normaliseEmail(email);
        this.passwordHash = passwordHash;
        this.fullName = requireText(fullName, "full name");
        this.status = Objects.requireNonNull(status);
        this.activationTokenHash = activationTokenHash;
        this.activationExpiresAt = activationExpiresAt;
        this.lastLoginAt = lastLoginAt;
        if (status == UserStatus.ACTIVE && passwordHash == null) {
            throw new IllegalArgumentException("An active user has a password");
        }
        this.lockVersion = lockVersion;
    }

    public static AppUser createAdministrator(UUID organizationId, String email, String fullName,
            String passwordHash) {
        return new AppUser(UuidV7.generate(), organizationId, email, passwordHash, fullName, UserStatus.ACTIVE,
                null, null, null, 0);
    }

    public static AppUser invite(UUID organizationId, String email, String fullName,
            String activationTokenHash, Instant activationExpiresAt) {
        return new AppUser(UuidV7.generate(), organizationId, email, null, fullName, UserStatus.INVITED,
                Objects.requireNonNull(activationTokenHash), Objects.requireNonNull(activationExpiresAt), null, 0);
    }

    public static AppUser restore(UUID id, UUID organizationId, String email, String passwordHash,
            String fullName, UserStatus status, String activationTokenHash, Instant activationExpiresAt,
            Instant lastLoginAt, long lockVersion) {
        return new AppUser(id, organizationId, email, passwordHash, fullName, status, activationTokenHash,
                activationExpiresAt, lastLoginAt, lockVersion);
    }

    public AppUser activate(String newPasswordHash) {
        return new AppUser(id, organizationId, email, Objects.requireNonNull(newPasswordHash), fullName,
                UserStatus.ACTIVE, null, null, lastLoginAt, lockVersion);
    }

    public AppUser suspend() {
        return new AppUser(id, organizationId, email, passwordHash, fullName, UserStatus.SUSPENDED,
                activationTokenHash, activationExpiresAt, lastLoginAt, lockVersion);
    }

    public AppUser recordLogin(Instant when) {
        return new AppUser(id, organizationId, email, passwordHash, fullName, status, activationTokenHash,
                activationExpiresAt, Objects.requireNonNull(when), lockVersion);
    }

    public boolean active() {
        return status == UserStatus.ACTIVE;
    }

    public boolean activationExpiredAt(Instant when) {
        return activationExpiresAt == null || !activationExpiresAt.isAfter(when);
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String fullName() {
        return fullName;
    }

    public UserStatus status() {
        return status;
    }

    public String activationTokenHash() {
        return activationTokenHash;
    }

    public Instant activationExpiresAt() {
        return activationExpiresAt;
    }

    public Instant lastLoginAt() {
        return lastLoginAt;
    }

    private static String normaliseEmail(String value) {
        String email = requireText(value, "email");
        if (!email.contains("@")) {
            throw new IllegalArgumentException("An email address needs an @, received " + email);
        }
        return email.toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A user needs a " + field);
        }
        return value.trim();
    }

    public long lockVersion() {
        return lockVersion;
    }
}
