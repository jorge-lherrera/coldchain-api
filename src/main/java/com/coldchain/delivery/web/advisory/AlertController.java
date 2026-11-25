package com.coldchain.delivery.web.advisory;

import com.coldchain.delivery.web.advisory.dto.ExcursionAlertResponse;
import com.coldchain.delivery.web.advisory.mapper.AdvisoryWebMapper;
import com.coldchain.modules.advisory.api.AdvisoryApi;
import com.coldchain.shared.response.ApiResponse;
import com.coldchain.shared.response.ApiResponseFactory;
import com.coldchain.shared.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Advisory", description = "Advice about a shipment's excursions, kept apart from its verdict")
@RequestMapping("/v1/shipments/{shipmentId}/alerts")
public class AlertController {

    private final AdvisoryApi advisory;

    private final AdvisoryWebMapper mapper;

    private final ApiResponseFactory responses;

    private final CurrentActor currentActor;

    public AlertController(AdvisoryApi advisory, AdvisoryWebMapper mapper, ApiResponseFactory responses,
            CurrentActor currentActor) {
        this.advisory = advisory;
        this.mapper = mapper;
        this.responses = responses;
        this.currentActor = currentActor;
    }

    @GetMapping
    @Operation(summary = "The alerts about a shipment's excursions",
            description = "Advice, not evidence: a probable cause always comes with how sure the triage "
                    + "was and whether a model or the rules produced it. The summary is in the language "
                    + "of Accept-Language, English when it names none of English, Spanish and Portuguese.")
    @PreAuthorize("hasAuthority('SCOPE_TELEMETRY_READ')")
    public ResponseEntity<ApiResponse<List<ExcursionAlertResponse>>> alerts(@PathVariable UUID shipmentId) {
        return responses.respond(AdvisorySuccessCode.ALERTS_RETRIEVED,
                mapper.toResponses(advisory.alertsOf(shipmentId, currentActor.requireOrganizationId())));
    }
}
