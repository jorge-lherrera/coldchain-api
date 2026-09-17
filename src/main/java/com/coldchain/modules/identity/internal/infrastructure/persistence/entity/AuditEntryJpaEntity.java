package com.coldchain.modules.identity.internal.infrastructure.persistence.entity;

import com.coldchain.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "AUDIT_ENTRY", indexes = {
        @Index(name = "IX_AUDIT_ENTRY_ORGANIZATION_OCCURRED_AT",
                columnList = "ORGANIZATION_ID, OCCURRED_AT"),
        @Index(name = "IX_AUDIT_ENTRY_RESOURCE", columnList = "RESOURCE_TYPE, RESOURCE_ID")})
public class AuditEntryJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ORGANIZATION_ID", nullable = false)
    private UUID organizationId;

    @Column(name = "ACTOR_TYPE", nullable = false)
    private String actorType;

    @Column(name = "ACTOR_ID", nullable = false)
    private UUID actorId;

    @Column(name = "ACTION", nullable = false)
    private String action;

    @Column(name = "RESOURCE_TYPE", nullable = false)
    private String resourceType;

    @Column(name = "RESOURCE_ID")
    private UUID resourceId;

    @Column(name = "PAYLOAD")
    private String payload;

    @Column(name = "OCCURRED_AT", nullable = false)
    private Instant occurredAt;

    protected AuditEntryJpaEntity() {
    }

    public AuditEntryJpaEntity(UUID id, UUID organizationId, String actorType, UUID actorId, String action, String resourceType, UUID resourceId, String payload, Instant occurredAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.actorType = actorType;
        this.actorId = actorId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getActorType() {
        return actorType;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
