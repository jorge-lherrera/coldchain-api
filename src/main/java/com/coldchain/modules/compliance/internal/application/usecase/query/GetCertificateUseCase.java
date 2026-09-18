package com.coldchain.modules.compliance.internal.application.usecase.query;

import com.coldchain.modules.compliance.api.dto.CertificateResult;
import com.coldchain.modules.compliance.internal.application.mapper.ComplianceApiMapper;
import com.coldchain.modules.compliance.internal.domain.repository.CertificateRepository;
import com.coldchain.modules.compliance.internal.exception.ComplianceErrorCode;
import com.coldchain.modules.shipment.api.ShipmentApi;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.util.UUID;

@UseCase
public class GetCertificateUseCase {

    private final CertificateRepository certificates;

    private final ShipmentApi shipments;

    private final ComplianceApiMapper mapper;

    public GetCertificateUseCase(CertificateRepository certificates, ShipmentApi shipments,
            ComplianceApiMapper mapper) {
        this.certificates = certificates;
        this.shipments = shipments;
        this.mapper = mapper;
    }

    public CertificateResult execute(UUID shipmentId, UUID organizationId) {
        shipments.shipmentOf(shipmentId, organizationId);
        return certificates.findCurrent(shipmentId).map(mapper::toResult)
                .orElseThrow(() -> DomainException.of(ComplianceErrorCode.CERTIFICATE_NOT_FOUND));
    }
}
