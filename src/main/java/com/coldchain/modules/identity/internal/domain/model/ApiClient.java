package com.coldchain.modules.identity.internal.domain.model;

import com.coldchain.modules.identity.api.ApiClientStatus;
import com.coldchain.modules.identity.api.Scope;
import com.coldchain.shared.identifier.UuidV7;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ApiClient {

    private final UUID id;

    private final UUID organizationId;

    private final String clientId;

    private final String secretHash;

    private final String label;

    private final ApiClientStatus status;

    private final Instant lastUsedAt;

    private final Set<Scope> scopes;

    private ApiClient(UUID id, UUID organizationId, String clientId, String secretHash, String label,
            ApiClientStatus status, Instant lastUsedAt, Set<Scope> scopes) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.clientId = requireText(clientId, "client identifier");
        this.secretHash = requireText(secretHash, "secret");
        this.label = requireText(label, "label");
        this.status = Objects.requireNonNull(status);
        this.lastUsedAt = lastUsedAt;
        this.scopes = Set.copyOf(Objects.requireNonNull(scopes));
    }

    public static ApiClient createNew(UUID organizationId, String clientId, String secretHash, String label,
            Set<Scope> scopes) {
        return new ApiClient(UuidV7.generate(), organizationId, clientId, secretHash, label,
                ApiClientStatus.ACTIVE, null, scopes);
    }

    public static ApiClient restore(UUID id, UUID organizationId, String clientId, String secretHash,
            String label, ApiClientStatus status, Instant lastUsedAt, Set<Scope> scopes) {
        return new ApiClient(id, organizationId, clientId, secretHash, label, status, lastUsedAt, scopes);
    }

    public ApiClient recordUse(Instant when) {
        return new ApiClient(id, organizationId, clientId, secretHash, label, status,
                Objects.requireNonNull(when), scopes);
    }

    public ApiClient revoke() {
        return new ApiClient(id, organizationId, clientId, secretHash, label, ApiClientStatus.REVOKED,
                lastUsedAt, scopes);
    }

    public boolean active() {
        return status == ApiClientStatus.ACTIVE;
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public String clientId() {
        return clientId;
    }

    public String secretHash() {
        return secretHash;
    }

    public String label() {
        return label;
    }

    public ApiClientStatus status() {
        return status;
    }

    public Instant lastUsedAt() {
        return lastUsedAt;
    }

    public Set<Scope> scopes() {
        return scopes;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A machine credential needs a " + field);
        }
        return value.trim();
    }
}
