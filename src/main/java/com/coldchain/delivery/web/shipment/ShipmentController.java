package com.coldchain.delivery.web.shipment;

import com.coldchain.delivery.web.shipment.dto.AcceptHandoffRequest;
import com.coldchain.delivery.web.shipment.dto.AddParticipantRequest;
import com.coldchain.delivery.web.shipment.dto.CreateShipmentRequest;
import com.coldchain.delivery.web.shipment.dto.DispatchShipmentRequest;
import com.coldchain.delivery.web.shipment.dto.OpenHandoffRequest;
import com.coldchain.delivery.web.shipment.dto.OpenHandoffResponse;
import com.coldchain.delivery.web.shipment.dto.ShipmentResponse;
import com.coldchain.delivery.web.shipment.dto.ShipmentTimelineResponse;
import com.coldchain.modules.shipment.api.ShipmentApi;
import com.coldchain.shared.pagination.SortCatalog;
import com.coldchain.shared.response.ApiResponse;
import com.coldchain.shared.response.ApiResponseFactory;
import com.coldchain.shared.security.CurrentActor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Shipments", description = "What moves, who holds it and how custody changes hands")
@RequestMapping("/v1/shipments")
public class ShipmentController {

    private static final SortCatalog<ShipmentSortField> SORTABLE =
            SortCatalog.of(ShipmentSortField.class, ShipmentSortField.REFERENCE, Sort.Direction.ASC);

    private final ShipmentApi shipments;

    private final ShipmentWebMapper mapper;

    private final ApiResponseFactory responses;

    private final CurrentActor currentActor;

    public ShipmentController(ShipmentApi shipments, ShipmentWebMapper mapper,
            ApiResponseFactory responses, CurrentActor currentActor) {
        this.shipments = shipments;
        this.mapper = mapper;
        this.responses = responses;
        this.currentActor = currentActor;
    }

    @PostMapping
    @Operation(summary = "Create a shipment",
            description = "Born in DRAFT. Each line copies the product name and the profile version, "
                    + "so what was shipped stays readable after the catalogue moves on.")
    @PreAuthorize("hasAuthority('SCOPE_SHIPMENT_WRITE')")
    public ResponseEntity<ApiResponse<ShipmentResponse>> create(
            @Valid @RequestBody CreateShipmentRequest request) {
        return responses.respond(ShipmentSuccessCode.SHIPMENT_CREATED,
                mapper.toResponse(shipments.createShipment(
                        mapper.toCommand(currentActor.requireOrganizationId(), request))));
    }

    @PostMapping("/{shipmentId}/dispatch")
    @Operation(summary = "Dispatch a shipment",
            description = "Freezes the thresholds onto the shipment and opens the monitoring window. "
                    + "A later change to the storage profile cannot rewrite what was promised here.")
    @PreAuthorize("hasAuthority('SCOPE_SHIPMENT_DISPATCH')")
    public ResponseEntity<ApiResponse<ShipmentResponse>> dispatch(@PathVariable UUID shipmentId,
            @Valid @RequestBody DispatchShipmentRequest request) {
        return responses.respond(ShipmentSuccessCode.SHIPMENT_DISPATCHED,
                mapper.toResponse(shipments.dispatchShipment(mapper.toCommand(shipmentId, request))));
    }

    @PostMapping("/{shipmentId}/arrival")
    @Operation(summary = "Record that the shipment reached its destination")
    @PreAuthorize("hasAuthority('SCOPE_SHIPMENT_WRITE')")
    public ResponseEntity<ApiResponse<ShipmentResponse>> arrive(@PathVariable UUID shipmentId) {
        return responses.respond(ShipmentSuccessCode.SHIPMENT_ARRIVED,
                mapper.toResponse(shipments.arriveShipment(shipmentId)));
    }

    @PostMapping("/{shipmentId}/delivery")
    @Operation(summary = "Accept the delivery",
            description = "Custody moves to the consignee and the shipment closes.")
    @PreAuthorize("hasAuthority('SCOPE_SHIPMENT_WRITE')")
    public ResponseEntity<ApiResponse<ShipmentResponse>> deliver(@PathVariable UUID shipmentId) {
        return responses.respond(ShipmentSuccessCode.SHIPMENT_DELIVERED,
                mapper.toResponse(shipments.deliverShipment(shipmentId)));
    }

    @PostMapping("/{shipmentId}/rejection")
    @Operation(summary = "Refuse the delivery")
    @PreAuthorize("hasAuthority('SCOPE_SHIPMENT_WRITE')")
    public ResponseEntity<ApiResponse<ShipmentResponse>> reject(@PathVariable UUID shipmentId) {
        return responses.respond(ShipmentSuccessCode.SHIPMENT_REJECTED,
                mapper.toResponse(shipments.rejectShipment(shipmentId)));
    }

    @PostMapping("/{shipmentId}/cancellation")
    @Operation(summary = "Cancel a shipment that never left")
    @PreAuthorize("hasAuthority('SCOPE_SHIPMENT_WRITE')")
    public ResponseEntity<ApiResponse<ShipmentResponse>> cancel(@PathVariable UUID shipmentId) {
        return responses.respond(ShipmentSuccessCode.SHIPMENT_CANCELLED,
                mapper.toResponse(shipments.cancelShipment(shipmentId)));
    }

    @PostMapping("/{shipmentId}/handoffs")
    @Operation(summary = "Open a handoff",
            description = "Returns a single-use code once. Only its hash is stored, so it cannot be "
                    + "looked up again, and opening the request moves no custody by itself.")
    @PreAuthorize("hasAuthority('SCOPE_SHIPMENT_HANDOFF')")
    public ResponseEntity<ApiResponse<OpenHandoffResponse>> openHandoff(@PathVariable UUID shipmentId,
            @Valid @RequestBody OpenHandoffRequest request) {
        return responses.respond(ShipmentSuccessCode.HANDOFF_OPENED,
                mapper.toResponse(shipments.openHandoff(mapper.toCommand(shipmentId, request))));
    }

    @PostMapping("/{shipmentId}/handoffs/acceptance")
    @Operation(summary = "Accept a handoff with the code",
            description = "This is the only place custody changes hands.")
    @PreAuthorize("hasAuthority('SCOPE_SHIPMENT_HANDOFF')")
    public ResponseEntity<ApiResponse<ShipmentResponse>> acceptHandoff(@PathVariable UUID shipmentId,
            @Valid @RequestBody AcceptHandoffRequest request) {
        return responses.respond(ShipmentSuccessCode.HANDOFF_ACCEPTED,
                mapper.toResponse(shipments.acceptHandoff(mapper.toCommand(shipmentId,
                        currentActor.requireOrganizationId(), request))));
    }

    @DeleteMapping("/{shipmentId}/handoffs")
    @Operation(summary = "Reject the pending handoff")
    @PreAuthorize("hasAuthority('SCOPE_SHIPMENT_HANDOFF')")
    public ResponseEntity<ApiResponse<Void>> rejectHandoff(@PathVariable UUID shipmentId) {
        shipments.rejectHandoff(shipmentId);
        return responses.respond(ShipmentSuccessCode.HANDOFF_REJECTED, null);
    }

    @PostMapping("/{shipmentId}/participants")
    @Operation(summary = "Let another organization see the shipment",
            description = "Only the shipper may change who takes part.")
    @PreAuthorize("hasAuthority('SCOPE_SHIPMENT_WRITE')")
    public ResponseEntity<ApiResponse<ShipmentResponse>> addParticipant(@PathVariable UUID shipmentId,
            @Valid @RequestBody AddParticipantRequest request) {
        return responses.respond(ShipmentSuccessCode.PARTICIPANT_ADDED,
                mapper.toResponse(shipments.addParticipant(mapper.toCommand(shipmentId, request))));
    }

    @DeleteMapping("/{shipmentId}/participants/{organizationId}")
    @Operation(summary = "Close an organization's window on the shipment",
            description = "It writes a revocation rather than deleting a row: who saw what is history.")
    @PreAuthorize("hasAuthority('SCOPE_SHIPMENT_WRITE')")
    public ResponseEntity<ApiResponse<Void>> revokeParticipant(@PathVariable UUID shipmentId,
            @PathVariable UUID organizationId) {
        shipments.revokeParticipant(shipmentId, organizationId);
        return responses.respond(ShipmentSuccessCode.PARTICIPANT_REVOKED, null);
    }

    @GetMapping("/{shipmentId}")
    @Operation(summary = "Read one shipment",
            description = "A shipment you do not take part in answers 404, not 403: a 403 would "
                    + "confirm it exists.")
    @PreAuthorize("hasAuthority('SCOPE_SHIPMENT_READ')")
    public ResponseEntity<ApiResponse<ShipmentResponse>> one(@PathVariable UUID shipmentId) {
        return responses.respond(ShipmentSuccessCode.SHIPMENT_RETRIEVED,
                mapper.toResponse(shipments.shipmentOf(shipmentId,
                        currentActor.requireOrganizationId())));
    }

    @GetMapping("/{shipmentId}/timeline")
    @Operation(summary = "The custody log and its chain verdict",
            description = "Every event chains against the previous hash, so altering one by hand "
                    + "shows up here.")
    @PreAuthorize("hasAuthority('SCOPE_SHIPMENT_READ')")
    public ResponseEntity<ApiResponse<ShipmentTimelineResponse>> timeline(
            @PathVariable UUID shipmentId) {
        return responses.respond(ShipmentSuccessCode.TIMELINE_RETRIEVED,
                mapper.toResponse(shipments.timelineOf(shipmentId,
                        currentActor.requireOrganizationId())));
    }

    @GetMapping
    @Operation(summary = "List the shipments the organization takes part in")
    @PreAuthorize("hasAuthority('SCOPE_SHIPMENT_READ')")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> list(Pageable pageable) {
        return responses.paginated(ShipmentSuccessCode.SHIPMENT_LISTED,
                shipments.listShipments(currentActor.requireOrganizationId(),
                        SORTABLE.apply(pageable)).map(mapper::toResponse));
    }
}
