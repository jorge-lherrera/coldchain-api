package com.coldchain.modules.shipment.internal.infrastructure.persistence.entity;

import com.coldchain.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "SHIPMENT", indexes = {
        @Index(name = "IX_SHIPMENT_ORGANIZATION_ID", columnList = "ORGANIZATION_ID")})
public class ShipmentJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ORGANIZATION_ID", nullable = false)
    private UUID organizationId;

    @Column(name = "REFERENCE", nullable = false)
    private String reference;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "ORIGIN_SITE_ID", nullable = false)
    private UUID originSiteId;

    @Column(name = "DESTINATION_SITE_ID", nullable = false)
    private UUID destinationSiteId;

    @Column(name = "CONSIGNEE_ORGANIZATION_ID", nullable = false)
    private UUID consigneeOrganizationId;

    @Column(name = "CURRENT_CUSTODIAN_ORGANIZATION_ID", nullable = false)
    private UUID currentCustodianOrganizationId;

    @Column(name = "DEVICE_ID")
    private UUID deviceId;

    @Column(name = "MIN_CELSIUS")
    private BigDecimal minCelsius;

    @Column(name = "MAX_CELSIUS")
    private BigDecimal maxCelsius;

    @Column(name = "MAX_SINGLE_EXCURSION_MINUTES")
    private Integer maxSingleExcursionMinutes;

    @Column(name = "MAX_CUMULATIVE_EXCURSION_MINUTES")
    private Integer maxCumulativeExcursionMinutes;

    @Column(name = "MIN_COVERAGE_PERCENT")
    private BigDecimal minCoveragePercent;

    @Column(name = "OPEN_EXCURSION", nullable = false)
    private Integer openExcursion;

    @Column(name = "DISPATCHED_AT")
    private Instant dispatchedAt;

    @Column(name = "CLOSED_AT")
    private Instant closedAt;

    @Version
    @Column(name = "LOCK_VERSION", nullable = false)
    private long lockVersion;

    protected ShipmentJpaEntity() {
    }

    public ShipmentJpaEntity(UUID id, UUID organizationId, String reference, String status, UUID originSiteId, UUID destinationSiteId, UUID consigneeOrganizationId, UUID currentCustodianOrganizationId, UUID deviceId, BigDecimal minCelsius, BigDecimal maxCelsius, Integer maxSingleExcursionMinutes, Integer maxCumulativeExcursionMinutes, BigDecimal minCoveragePercent, Integer openExcursion, Instant dispatchedAt, Instant closedAt, long lockVersion) {
        this.id = id;
        this.organizationId = organizationId;
        this.reference = reference;
        this.status = status;
        this.originSiteId = originSiteId;
        this.destinationSiteId = destinationSiteId;
        this.consigneeOrganizationId = consigneeOrganizationId;
        this.currentCustodianOrganizationId = currentCustodianOrganizationId;
        this.deviceId = deviceId;
        this.minCelsius = minCelsius;
        this.maxCelsius = maxCelsius;
        this.maxSingleExcursionMinutes = maxSingleExcursionMinutes;
        this.maxCumulativeExcursionMinutes = maxCumulativeExcursionMinutes;
        this.minCoveragePercent = minCoveragePercent;
        this.openExcursion = openExcursion;
        this.dispatchedAt = dispatchedAt;
        this.closedAt = closedAt;
        this.lockVersion = lockVersion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getReference() {
        return reference;
    }

    public String getStatus() {
        return status;
    }

    public UUID getOriginSiteId() {
        return originSiteId;
    }

    public UUID getDestinationSiteId() {
        return destinationSiteId;
    }

    public UUID getConsigneeOrganizationId() {
        return consigneeOrganizationId;
    }

    public UUID getCurrentCustodianOrganizationId() {
        return currentCustodianOrganizationId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public BigDecimal getMinCelsius() {
        return minCelsius;
    }

    public BigDecimal getMaxCelsius() {
        return maxCelsius;
    }

    public Integer getMaxSingleExcursionMinutes() {
        return maxSingleExcursionMinutes;
    }

    public Integer getMaxCumulativeExcursionMinutes() {
        return maxCumulativeExcursionMinutes;
    }

    public BigDecimal getMinCoveragePercent() {
        return minCoveragePercent;
    }

    public Integer getOpenExcursion() {
        return openExcursion;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}
