package com.coldchain.modules.catalog.internal.infrastructure.persistence.entity;

import com.coldchain.shared.domain.AuditableEntity;
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
@Table(name = "STORAGE_PROFILE", indexes = {
        @Index(name = "IX_STORAGE_PROFILE_ORGANIZATION_ID", columnList = "ORGANIZATION_ID")})
public class StorageProfileJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ORGANIZATION_ID", nullable = false)
    private UUID organizationId;

    @Column(name = "CODE", nullable = false)
    private String code;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "PROFILE_VERSION", nullable = false)
    private int profileVersion;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "MIN_CELSIUS", nullable = false)
    private BigDecimal minCelsius;

    @Column(name = "MAX_CELSIUS", nullable = false)
    private BigDecimal maxCelsius;

    @Column(name = "MAX_SINGLE_EXCURSION_MINUTES", nullable = false)
    private int maxSingleExcursionMinutes;

    @Column(name = "MAX_CUMULATIVE_EXCURSION_MINUTES", nullable = false)
    private int maxCumulativeExcursionMinutes;

    @Column(name = "MIN_COVERAGE_PERCENT", nullable = false)
    private BigDecimal minCoveragePercent;

    @Column(name = "DELETED_AT")
    private Instant deletedAt;

    @Version
    @Column(name = "LOCK_VERSION", nullable = false)
    private long lockVersion;

    protected StorageProfileJpaEntity() {
    }

    public StorageProfileJpaEntity(UUID id, UUID organizationId, String code, String name,
            int profileVersion, String status, BigDecimal minCelsius, BigDecimal maxCelsius,
            int maxSingleExcursionMinutes, int maxCumulativeExcursionMinutes,
            BigDecimal minCoveragePercent, Instant deletedAt, long lockVersion) {
        this.id = id;
        this.organizationId = organizationId;
        this.code = code;
        this.name = name;
        this.profileVersion = profileVersion;
        this.status = status;
        this.minCelsius = minCelsius;
        this.maxCelsius = maxCelsius;
        this.maxSingleExcursionMinutes = maxSingleExcursionMinutes;
        this.maxCumulativeExcursionMinutes = maxCumulativeExcursionMinutes;
        this.minCoveragePercent = minCoveragePercent;
        this.deletedAt = deletedAt;
        this.lockVersion = lockVersion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getProfileVersion() {
        return profileVersion;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getMinCelsius() {
        return minCelsius;
    }

    public BigDecimal getMaxCelsius() {
        return maxCelsius;
    }

    public int getMaxSingleExcursionMinutes() {
        return maxSingleExcursionMinutes;
    }

    public int getMaxCumulativeExcursionMinutes() {
        return maxCumulativeExcursionMinutes;
    }

    public BigDecimal getMinCoveragePercent() {
        return minCoveragePercent;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}
