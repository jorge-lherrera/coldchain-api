package com.coldchain.modules.compliance.api;

import com.coldchain.modules.compliance.api.dto.CertificateResult;
import java.util.UUID;

public interface ComplianceApi {

    CertificateResult issueCertificate(UUID shipmentId, UUID organizationId);

    CertificateResult currentCertificateOf(UUID shipmentId, UUID organizationId);
}
