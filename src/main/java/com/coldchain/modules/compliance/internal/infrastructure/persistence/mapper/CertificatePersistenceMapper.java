package com.coldchain.modules.compliance.internal.infrastructure.persistence.mapper;

import com.coldchain.modules.compliance.api.FindingCode;
import com.coldchain.modules.compliance.api.Severity;
import com.coldchain.modules.compliance.api.Verdict;
import com.coldchain.modules.compliance.internal.domain.model.ComplianceCertificate;
import com.coldchain.modules.compliance.internal.domain.model.Finding;
import com.coldchain.modules.compliance.internal.infrastructure.persistence.entity.CertificateFindingJpaEntity;
import com.coldchain.modules.compliance.internal.infrastructure.persistence.entity.ComplianceCertificateJpaEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CertificatePersistenceMapper {

    public ComplianceCertificateJpaEntity toEntity(ComplianceCertificate certificate) {
        return new ComplianceCertificateJpaEntity(certificate.id(), certificate.organizationId(),
                certificate.shipmentId(), certificate.version(), certificate.verdict().name(),
                certificate.coveragePercent(), certificate.cumulativeExcursionMinutes(),
                certificate.longestExcursionMinutes(), certificate.thresholdSnapshot(),
                certificate.evaluatedFrom(), certificate.evaluatedTo(), certificate.issuedAt(),
                certificate.contentHash(), certificate.supersededAt());
    }

    public CertificateFindingJpaEntity toEntity(Finding finding) {
        return new CertificateFindingJpaEntity(finding.id(), finding.certificateId(),
                finding.code().name(), finding.severity().name(), finding.excursionId(),
                finding.detail());
    }

    public ComplianceCertificate toDomain(ComplianceCertificateJpaEntity entity,
            List<CertificateFindingJpaEntity> findings) {
        return ComplianceCertificate.restore(entity.getId(), entity.getOrganizationId(),
                entity.getShipmentId(), entity.getCertificateVersion(),
                Verdict.valueOf(entity.getVerdict()), entity.getCoveragePercent(),
                entity.getCumulativeMinutes(), entity.getLongestMinutes(),
                entity.getThresholdSnapshot(), entity.getEvaluatedFrom(), entity.getEvaluatedTo(),
                entity.getIssuedAt(), entity.getContentHash(), entity.getSupersededAt(),
                findings.stream().map(this::toDomain).toList());
    }

    public Finding toDomain(CertificateFindingJpaEntity entity) {
        return Finding.restore(entity.getId(), entity.getCertificateId(),
                FindingCode.valueOf(entity.getCode()), Severity.valueOf(entity.getSeverity()),
                entity.getExcursionId(), entity.getDetail());
    }
}
