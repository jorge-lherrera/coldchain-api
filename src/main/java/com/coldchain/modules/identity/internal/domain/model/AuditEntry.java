package com.coldchain.modules.identity.internal.domain.model;

import com.coldchain.modules.identity.api.ActorType;
import com.coldchain.shared.identifier.UuidV7;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AuditEntry {

    private final UUID id;

    private final UUID organizationId;

    private final ActorType actorType;

    private final UUID actorId;

    private final String action;

    private final String resourceType;

    private final UUID resourceId;

    private final String payload;

    private final Instant occurredAt;

    private AuditEntry(UUID id, UUID organizationId, ActorType actorType, UUID actorId, String action,
            String resourceType, UUID resourceId, String payload, Instant occurredAt) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.actorType = Objects.requireNonNull(actorType);
        this.actorId = Objects.requireNonNull(actorId);
        this.action = Objects.requireNonNull(action);
        this.resourceType = Objects.requireNonNull(resourceType);
        this.resourceId = resourceId;
        this.payload = payload;
        this.occurredAt = Objects.requireNonNull(occurredAt);
    }

    public static AuditEntry createNew(UUID organizationId, ActorType actorType, UUID actorId, String action,
            String resourceType, UUID resourceId, String payload, Instant occurredAt) {
        return new AuditEntry(UuidV7.generate(), organizationId, actorType, actorId, action, resourceType,
                resourceId, payload, occurredAt);
    }

    public static AuditEntry restore(UUID id, UUID organizationId, ActorType actorType, UUID actorId,
            String action, String resourceType, UUID resourceId, String payload, Instant occurredAt) {
        return new AuditEntry(id, organizationId, actorType, actorId, action, resourceType, resourceId,
                payload, occurredAt);
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public ActorType actorType() {
        return actorType;
    }

    public UUID actorId() {
        return actorId;
    }

    public String action() {
        return action;
    }

    public String resourceType() {
        return resourceType;
    }

    public UUID resourceId() {
        return resourceId;
    }

    public String payload() {
        return payload;
    }

    public Instant occurredAt() {
        return occurredAt;
    }
}
