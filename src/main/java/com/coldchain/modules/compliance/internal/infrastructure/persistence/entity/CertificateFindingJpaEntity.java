package com.coldchain.modules.compliance.internal.infrastructure.persistence.entity;

import com.coldchain.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "CERTIFICATE_FINDING", indexes = {
        @Index(name = "IX_CERTIFICATE_FINDING_CERTIFICATE_ID", columnList = "CERTIFICATE_ID")})
public class CertificateFindingJpaEntity extends AuditableEntity {

    @Id
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "CERTIFICATE_ID", nullable = false)
    private UUID certificateId;

    @Column(name = "CODE", nullable = false)
    private String code;

    @Column(name = "SEVERITY", nullable = false)
    private String severity;

    @Column(name = "EXCURSION_ID")
    private UUID excursionId;

    @Column(name = "DETAIL")
    private String detail;

    protected CertificateFindingJpaEntity() {
    }

    public CertificateFindingJpaEntity(UUID id, UUID certificateId, String code, String severity,
            UUID excursionId, String detail) {
        this.id = id;
        this.certificateId = certificateId;
        this.code = code;
        this.severity = severity;
        this.excursionId = excursionId;
        this.detail = detail;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCertificateId() {
        return certificateId;
    }

    public String getCode() {
        return code;
    }

    public String getSeverity() {
        return severity;
    }

    public UUID getExcursionId() {
        return excursionId;
    }

    public String getDetail() {
        return detail;
    }
}
