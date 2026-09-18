package com.coldchain.modules.shipment.internal.domain.model;

import com.coldchain.modules.shipment.api.HandoffStatus;
import com.coldchain.shared.identifier.UuidV7;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class HandoffRequest {

    private final UUID id;

    private final UUID shipmentId;

    private final UUID fromOrganizationId;

    private final UUID toOrganizationId;

    private final String codeHash;

    private final HandoffStatus status;

    private final Instant expiresAt;

    private final Instant resolvedAt;

    private final long lockVersion;

    private HandoffRequest(UUID id, UUID shipmentId, UUID fromOrganizationId, UUID toOrganizationId,
            String codeHash, HandoffStatus status, Instant expiresAt, Instant resolvedAt, long lockVersion) {
        this.id = Objects.requireNonNull(id);
        this.shipmentId = Objects.requireNonNull(shipmentId);
        this.fromOrganizationId = Objects.requireNonNull(fromOrganizationId);
        this.toOrganizationId = Objects.requireNonNull(toOrganizationId);
        this.codeHash = Objects.requireNonNull(codeHash);
        this.status = Objects.requireNonNull(status);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.resolvedAt = resolvedAt;
        this.lockVersion = lockVersion;
    }

    public static HandoffRequest open(UUID shipmentId, UUID fromOrganizationId, UUID toOrganizationId,
            String codeHash, Instant expiresAt) {
        return new HandoffRequest(UuidV7.generate(), shipmentId, fromOrganizationId, toOrganizationId,
                codeHash, HandoffStatus.PENDING, expiresAt, null, 0);
    }

    public static HandoffRequest restore(UUID id, UUID shipmentId, UUID fromOrganizationId,
            UUID toOrganizationId, String codeHash, HandoffStatus status, Instant expiresAt,
            Instant resolvedAt, long lockVersion) {
        return new HandoffRequest(id, shipmentId, fromOrganizationId, toOrganizationId, codeHash,
                status, expiresAt, resolvedAt, lockVersion);
    }

    public HandoffRequest accept(Instant when) {
        return new HandoffRequest(id, shipmentId, fromOrganizationId, toOrganizationId, codeHash,
                HandoffStatus.ACCEPTED, expiresAt, when, lockVersion);
    }

    public HandoffRequest reject(Instant when) {
        return new HandoffRequest(id, shipmentId, fromOrganizationId, toOrganizationId, codeHash,
                HandoffStatus.REJECTED, expiresAt, when, lockVersion);
    }

    public HandoffRequest expire(Instant when) {
        return new HandoffRequest(id, shipmentId, fromOrganizationId, toOrganizationId, codeHash,
                HandoffStatus.EXPIRED, expiresAt, when, lockVersion);
    }

    public boolean pending() {
        return status == HandoffStatus.PENDING;
    }

    public boolean expiredAt(Instant when) {
        return when.isAfter(expiresAt);
    }

    public UUID id() {
        return id;
    }

    public UUID shipmentId() {
        return shipmentId;
    }

    public UUID fromOrganizationId() {
        return fromOrganizationId;
    }

    public UUID toOrganizationId() {
        return toOrganizationId;
    }

    public String codeHash() {
        return codeHash;
    }

    public HandoffStatus status() {
        return status;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }

    public long lockVersion() {
        return lockVersion;
    }
}
