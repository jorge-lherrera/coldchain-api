package com.coldchain.modules.shipment.internal.domain.model;

import com.coldchain.modules.shipment.api.ShipmentStatus;
import com.coldchain.shared.util.UuidV7;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Shipment {

    private final UUID id;

    private final UUID organizationId;

    private final String reference;

    private final ShipmentStatus status;

    private final UUID originSiteId;

    private final UUID destinationSiteId;

    private final UUID consigneeOrganizationId;

    private final UUID currentCustodianOrganizationId;

    private final UUID deviceId;

    private final FrozenThresholds thresholds;

    private final boolean openExcursion;

    private final Instant dispatchedAt;

    private final Instant closedAt;

    private final List<ShipmentLine> lines;

    private final long lockVersion;

    private Shipment(UUID id, UUID organizationId, String reference, ShipmentStatus status,
            UUID originSiteId, UUID destinationSiteId, UUID consigneeOrganizationId,
            UUID currentCustodianOrganizationId, UUID deviceId, FrozenThresholds thresholds,
            boolean openExcursion, Instant dispatchedAt, Instant closedAt, List<ShipmentLine> lines, long lockVersion) {
        this.id = Objects.requireNonNull(id);
        this.organizationId = Objects.requireNonNull(organizationId);
        this.reference = Objects.requireNonNull(reference);
        this.status = Objects.requireNonNull(status);
        this.originSiteId = Objects.requireNonNull(originSiteId);
        this.destinationSiteId = Objects.requireNonNull(destinationSiteId);
        this.consigneeOrganizationId = Objects.requireNonNull(consigneeOrganizationId);
        this.currentCustodianOrganizationId = Objects.requireNonNull(currentCustodianOrganizationId);
        this.deviceId = deviceId;
        this.thresholds = thresholds;
        this.openExcursion = openExcursion;
        this.dispatchedAt = dispatchedAt;
        this.closedAt = closedAt;
        this.lines = List.copyOf(lines);
        this.lockVersion = lockVersion;
    }

    public static Shipment createDraft(UUID organizationId, String reference, UUID originSiteId,
            UUID destinationSiteId, UUID consigneeOrganizationId) {
        return new Shipment(UuidV7.generate(), organizationId, reference, ShipmentStatus.DRAFT,
                originSiteId, destinationSiteId, consigneeOrganizationId, organizationId, null, null,
                false, null, null, List.of(), 0);
    }

    public static Shipment restore(UUID id, UUID organizationId, String reference,
            ShipmentStatus status, UUID originSiteId, UUID destinationSiteId,
            UUID consigneeOrganizationId, UUID currentCustodianOrganizationId, UUID deviceId,
            FrozenThresholds thresholds, boolean openExcursion, Instant dispatchedAt,
            Instant closedAt, List<ShipmentLine> lines, long lockVersion) {
        return new Shipment(id, organizationId, reference, status, originSiteId, destinationSiteId,
                consigneeOrganizationId, currentCustodianOrganizationId, deviceId, thresholds,
                openExcursion, dispatchedAt, closedAt, lines, lockVersion);
    }

    public Shipment withLines(List<ShipmentLine> newLines) {
        return copyWith(status, currentCustodianOrganizationId, deviceId, thresholds, openExcursion,
                dispatchedAt, closedAt, newLines);
    }

    public Shipment dispatch(UUID assignedDeviceId, FrozenThresholds frozen, Instant when) {
        requireTransitionTo(ShipmentStatus.IN_TRANSIT);
        if (lines.isEmpty()) {
            throw new IllegalStateException("A shipment with no lines carries nothing");
        }
        if (assignedDeviceId == null) {
            throw new IllegalStateException("A shipment with no device cannot be monitored");
        }
        return copyWith(ShipmentStatus.IN_TRANSIT, currentCustodianOrganizationId, assignedDeviceId,
                frozen, openExcursion, when, closedAt, lines);
    }

    public Shipment arrive() {
        requireTransitionTo(ShipmentStatus.AT_DESTINATION);
        return copyWith(ShipmentStatus.AT_DESTINATION, currentCustodianOrganizationId, deviceId,
                thresholds, openExcursion, dispatchedAt, closedAt, lines);
    }

    public Shipment deliver(Instant when) {
        requireTransitionTo(ShipmentStatus.DELIVERED);
        return copyWith(ShipmentStatus.DELIVERED, consigneeOrganizationId, deviceId, thresholds,
                openExcursion, dispatchedAt, when, lines);
    }

    public Shipment reject(Instant when) {
        requireTransitionTo(ShipmentStatus.REJECTED);
        return copyWith(ShipmentStatus.REJECTED, currentCustodianOrganizationId, deviceId, thresholds,
                openExcursion, dispatchedAt, when, lines);
    }

    public Shipment cancel(Instant when) {
        requireTransitionTo(ShipmentStatus.CANCELLED);
        return copyWith(ShipmentStatus.CANCELLED, currentCustodianOrganizationId, deviceId, thresholds,
                openExcursion, dispatchedAt, when, lines);
    }

    public Shipment handOverTo(UUID organization) {
        if (status != ShipmentStatus.IN_TRANSIT) {
            throw new IllegalStateException("Custody only moves while the shipment is in transit");
        }
        return copyWith(status, organization, deviceId, thresholds, openExcursion, dispatchedAt,
                closedAt, lines);
    }

    public Shipment withOpenExcursion(boolean open) {
        return copyWith(status, currentCustodianOrganizationId, deviceId, thresholds, open,
                dispatchedAt, closedAt, lines);
    }

    private void requireTransitionTo(ShipmentStatus target) {
        if (!StatusTransitions.allows(status, target)) {
            throw new IllegalStateException(
                    "A shipment in " + status + " cannot move to " + target);
        }
    }

    private Shipment copyWith(ShipmentStatus newStatus, UUID custodian, UUID device,
            FrozenThresholds frozen, boolean excursion, Instant dispatched, Instant closed,
            List<ShipmentLine> newLines) {
        return new Shipment(id, organizationId, reference, newStatus, originSiteId, destinationSiteId,
                consigneeOrganizationId, custodian, device, frozen, excursion, dispatched, closed,
                newLines, lockVersion);
    }

    public UUID id() {
        return id;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public String reference() {
        return reference;
    }

    public ShipmentStatus status() {
        return status;
    }

    public UUID originSiteId() {
        return originSiteId;
    }

    public UUID destinationSiteId() {
        return destinationSiteId;
    }

    public UUID consigneeOrganizationId() {
        return consigneeOrganizationId;
    }

    public UUID currentCustodianOrganizationId() {
        return currentCustodianOrganizationId;
    }

    public UUID deviceId() {
        return deviceId;
    }

    public FrozenThresholds thresholds() {
        return thresholds;
    }

    public boolean openExcursion() {
        return openExcursion;
    }

    public Instant dispatchedAt() {
        return dispatchedAt;
    }

    public Instant closedAt() {
        return closedAt;
    }

    public List<ShipmentLine> lines() {
        return lines;
    }

    public long lockVersion() {
        return lockVersion;
    }
}
