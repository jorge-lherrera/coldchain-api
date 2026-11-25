package com.coldchain.modules.advisory.internal.infrastructure.persistence.entity;

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
@Table(name = "EXCURSION_ALERT", indexes = {
        @Index(name = "IX_EXCURSION_ALERT_ORGANIZATION_ID", columnList = "ORGANIZATION_ID"),
        @Index(name = "IX_EXCURSION_ALERT_SHIPMENT_ID_TRIAGED_AT", columnList = "SHIPMENT_ID, TRIAGED_AT")})
public class ExcursionAlertJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ORGANIZATION_ID", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "SHIPMENT_ID", nullable = false, updatable = false)
    private UUID shipmentId;

    @Column(name = "EXCURSION_ID", nullable = false, updatable = false)
    private UUID excursionId;

    @Column(name = "PROBABLE_CAUSE", nullable = false)
    private String probableCause;

    @Column(name = "SEVERITY", nullable = false)
    private String severity;

    @Column(name = "CONFIDENCE")
    private BigDecimal confidence;

    @Column(name = "SUMMARY_EN", nullable = false)
    private String summaryEn;

    @Column(name = "SUMMARY_ES", nullable = false)
    private String summaryEs;

    @Column(name = "SUMMARY_PT", nullable = false)
    private String summaryPt;

    @Column(name = "RECOMMENDED_ACTION", nullable = false)
    private String recommendedAction;

    @Column(name = "SOURCE", nullable = false)
    private String source;

    @Column(name = "MODEL")
    private String model;

    @Column(name = "CONTEXT_AVAILABLE", nullable = false)
    private boolean contextAvailable;

    @Column(name = "TRIAGED_AT", nullable = false)
    private Instant triagedAt;

    @Version
    @Column(name = "LOCK_VERSION", nullable = false)
    private long lockVersion;

    protected ExcursionAlertJpaEntity() {
    }

    public ExcursionAlertJpaEntity(UUID id, UUID organizationId, UUID shipmentId, UUID excursionId,
            String probableCause, String severity, BigDecimal confidence, String summaryEn, String summaryEs,
            String summaryPt, String recommendedAction, String source, String model, boolean contextAvailable,
            Instant triagedAt, long lockVersion) {
        this.id = id;
        this.organizationId = organizationId;
        this.shipmentId = shipmentId;
        this.excursionId = excursionId;
        this.probableCause = probableCause;
        this.severity = severity;
        this.confidence = confidence;
        this.summaryEn = summaryEn;
        this.summaryEs = summaryEs;
        this.summaryPt = summaryPt;
        this.recommendedAction = recommendedAction;
        this.source = source;
        this.model = model;
        this.contextAvailable = contextAvailable;
        this.triagedAt = triagedAt;
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

    public UUID getExcursionId() {
        return excursionId;
    }

    public String getProbableCause() {
        return probableCause;
    }

    public String getSeverity() {
        return severity;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getSummaryEn() {
        return summaryEn;
    }

    public String getSummaryEs() {
        return summaryEs;
    }

    public String getSummaryPt() {
        return summaryPt;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public String getSource() {
        return source;
    }

    public String getModel() {
        return model;
    }

    public boolean getContextAvailable() {
        return contextAvailable;
    }

    public Instant getTriagedAt() {
        return triagedAt;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}
