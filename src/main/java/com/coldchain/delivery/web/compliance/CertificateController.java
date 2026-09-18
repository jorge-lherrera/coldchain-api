package com.coldchain.delivery.web.compliance;

import com.coldchain.modules.compliance.api.ComplianceApi;
import com.coldchain.modules.compliance.api.dto.CertificateResult;
import com.coldchain.shared.response.ApiResponse;
import com.coldchain.shared.response.ResponseFactory;
import com.coldchain.shared.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Compliance", description = "The verdict on what a shipment promised and what it did")
@RequestMapping("/v1/shipments/{shipmentId}/certificate")
public class CertificateController {

    private final ComplianceApi compliance;

    private final ResponseFactory responses;

    private final CurrentActor currentActor;

    public CertificateController(ComplianceApi compliance, ResponseFactory responses,
            CurrentActor currentActor) {
        this.compliance = compliance;
        this.responses = responses;
        this.currentActor = currentActor;
    }

    @PostMapping
    @Operation(summary = "Issue the certificate of a shipment",
            description = "Judged against the thresholds the shipment froze, never against the "
                    + "profile as it stands today. Issuing again supersedes the previous version "
                    + "rather than overwriting it.")
    @PreAuthorize("hasAuthority('SCOPE_COMPLIANCE_ISSUE')")
    public ResponseEntity<ApiResponse<CertificateResult>> issue(@PathVariable UUID shipmentId) {
        return responses.respond(ComplianceSuccessCode.CERTIFICATE_ISSUED,
                compliance.issueCertificate(shipmentId, currentActor.requireOrganizationId()));
    }

    @GetMapping
    @Operation(summary = "Read the current certificate of a shipment")
    @PreAuthorize("hasAuthority('SCOPE_COMPLIANCE_READ')")
    public ResponseEntity<ApiResponse<CertificateResult>> current(@PathVariable UUID shipmentId) {
        return responses.respond(ComplianceSuccessCode.CERTIFICATE_RETRIEVED,
                compliance.currentCertificateOf(shipmentId, currentActor.requireOrganizationId()));
    }
}
