package com.coldchain.modules.compliance.internal.application;

import com.coldchain.modules.compliance.api.ComplianceApi;
import com.coldchain.modules.compliance.api.dto.CertificateResult;
import com.coldchain.modules.compliance.internal.application.usecase.command.IssueCertificateUseCase;
import com.coldchain.modules.compliance.internal.application.usecase.query.GetCertificateUseCase;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ComplianceFacade implements ComplianceApi {

    private final IssueCertificateUseCase issueCertificate;

    private final GetCertificateUseCase getCertificate;

    public ComplianceFacade(IssueCertificateUseCase issueCertificate,
            GetCertificateUseCase getCertificate) {
        this.issueCertificate = issueCertificate;
        this.getCertificate = getCertificate;
    }

    @Override
    public CertificateResult issueCertificate(UUID shipmentId, UUID organizationId) {
        return issueCertificate.execute(shipmentId, organizationId);
    }

    @Override
    public CertificateResult currentCertificateOf(UUID shipmentId, UUID organizationId) {
        return getCertificate.execute(shipmentId, organizationId);
    }
}
