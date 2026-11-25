package com.coldchain.delivery.web.shipment;

import com.coldchain.modules.shipment.api.ShipmentApi;
import com.coldchain.modules.shipment.api.dto.ExcursionContextResult;
import com.coldchain.shared.response.ApiResponse;
import com.coldchain.shared.response.ApiResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Platform", description = "The internal surface only the platform credential reads")
@RequestMapping("/internal/v1/shipments")
public class ExcursionContextController {

    private final ShipmentApi shipments;

    private final ApiResponseFactory responses;

    public ExcursionContextController(ShipmentApi shipments, ApiResponseFactory responses) {
        this.shipments = shipments;
        this.responses = responses;
    }

    @GetMapping("/{shipmentId}/excursions/{excursionId}/context")
    @Operation(summary = "What surrounded an excursion",
            description = "The readings from thirty minutes before it opened to thirty minutes after "
                    + "it closed, the frozen thresholds, and who held custody by participation. It "
                    + "names no organization and carries no identifier beyond the two in the path.")
    @PreAuthorize("hasAuthority('SCOPE_PLATFORM_EXCURSION_READ')")
    public ResponseEntity<ApiResponse<ExcursionContextResult>> context(@PathVariable UUID shipmentId,
            @PathVariable UUID excursionId) {
        return responses.respond(ShipmentSuccessCode.EXCURSION_CONTEXT_RETRIEVED,
                shipments.excursionContextOf(shipmentId, excursionId));
    }
}
