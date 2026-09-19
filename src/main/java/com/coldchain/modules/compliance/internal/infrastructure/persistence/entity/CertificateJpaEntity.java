package com.coldchain.modules.compliance.internal.infrastructure.persistence.entity;

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
@Table(name = "CERTIFICATE", indexes = {
        @Index(name = "IX_CERTIFICATE_ORGANIZATION_ID", columnList = "ORGANIZATION_ID"),
        @Index(name = "IX_CERTIFICATE_SHIPMENT_ID", columnList = "SHIPMENT_ID")})
public class CertificateJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ORGANIZATION_ID", nullable = false)
    private UUID organizationId;

    @Column(name = "SHIPMENT_ID", nullable = false)
    private UUID shipmentId;

    @Column(name = "CERTIFICATE_VERSION", nullable = false)
    private int certificateVersion;

    @Column(name = "VERDICT", nullable = false)
    private String verdict;

    @Column(name = "COVERAGE_PERCENT", nullable = false)
    private BigDecimal coveragePercent;

    @Column(name = "CUMULATIVE_MINUTES", nullable = false)
    private long cumulativeMinutes;

    @Column(name = "LONGEST_MINUTES", nullable = false)
    private long longestMinutes;

    @Column(name = "THRESHOLD_SNAPSHOT", nullable = false)
    private String thresholdSnapshot;

    @Column(name = "EVALUATED_FROM", nullable = false)
    private Instant evaluatedFrom;

    @Column(name = "EVALUATED_TO", nullable = false)
    private Instant evaluatedTo;

    @Column(name = "ISSUED_AT", nullable = false)
    private Instant issuedAt;

    @Column(name = "CONTENT_HASH", nullable = false)
    private String contentHash;

    @Column(name = "SUPERSEDED_AT")
    private Instant supersededAt;

    @Version
    @Column(name = "LOCK_VERSION", nullable = false)
    private long lockVersion;

    protected CertificateJpaEntity() {
    }

    public CertificateJpaEntity(UUID id, UUID organizationId, UUID shipmentId,
            int certificateVersion, String verdict, BigDecimal coveragePercent, long cumulativeMinutes,
            long longestMinutes, String thresholdSnapshot, Instant evaluatedFrom, Instant evaluatedTo,
            Instant issuedAt, String contentHash, Instant supersededAt, long lockVersion) {
        this.id = id;
        this.organizationId = organizationId;
        this.shipmentId = shipmentId;
        this.certificateVersion = certificateVersion;
        this.verdict = verdict;
        this.coveragePercent = coveragePercent;
        this.cumulativeMinutes = cumulativeMinutes;
        this.longestMinutes = longestMinutes;
        this.thresholdSnapshot = thresholdSnapshot;
        this.evaluatedFrom = evaluatedFrom;
        this.evaluatedTo = evaluatedTo;
        this.issuedAt = issuedAt;
        this.contentHash = contentHash;
        this.supersededAt = supersededAt;
        this.lockVersion = lockVersion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public int getCertificateVersion() {
        return certificateVersion;
    }

    public String getVerdict() {
        return verdict;
    }

    public BigDecimal getCoveragePercent() {
        return coveragePercent;
    }

    public long getCumulativeMinutes() {
        return cumulativeMinutes;
    }

    public long getLongestMinutes() {
        return longestMinutes;
    }

    public String getThresholdSnapshot() {
        return thresholdSnapshot;
    }

    public Instant getEvaluatedFrom() {
        return evaluatedFrom;
    }

    public Instant getEvaluatedTo() {
        return evaluatedTo;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public String getContentHash() {
        return contentHash;
    }

    public Instant getSupersededAt() {
        return supersededAt;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}
