package com.coldchain.modules.compliance.internal.application.mapper;

import com.coldchain.modules.compliance.api.dto.CertificateResult;
import com.coldchain.modules.compliance.api.dto.FindingResult;
import com.coldchain.modules.compliance.internal.domain.model.ComplianceCertificate;
import com.coldchain.modules.compliance.internal.domain.model.Finding;
import com.coldchain.modules.shipment.api.dto.ShipmentThresholds;
import org.springframework.stereotype.Component;

@Component
public class ComplianceApiMapper {

    public CertificateResult toResult(ComplianceCertificate certificate) {
        return new CertificateResult(certificate.id(), certificate.shipmentId(), certificate.version(),
                certificate.verdict(), certificate.coveragePercent(),
                certificate.cumulativeExcursionMinutes(), certificate.longestExcursionMinutes(),
                certificate.evaluatedFrom(), certificate.evaluatedTo(), certificate.issuedAt(),
                certificate.contentHash(),
                certificate.findings().stream().map(this::toResult).toList());
    }

    public FindingResult toResult(Finding finding) {
        return new FindingResult(finding.id(), finding.code(), finding.severity(),
                finding.excursionId(), finding.detail());
    }

    public String snapshotOf(ShipmentThresholds thresholds) {
        return "{\"minCelsius\":" + thresholds.minCelsius()
                + ",\"maxCelsius\":" + thresholds.maxCelsius()
                + ",\"maxSingleExcursionMinutes\":" + thresholds.maxSingleExcursionMinutes()
                + ",\"maxCumulativeExcursionMinutes\":" + thresholds.maxCumulativeExcursionMinutes()
                + ",\"minCoveragePercent\":" + thresholds.minCoveragePercent() + "}";
    }
}
