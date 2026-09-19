package com.coldchain.delivery.web.shipment.mapper;

import com.coldchain.delivery.web.shipment.dto.AcceptHandoffRequest;
import com.coldchain.delivery.web.shipment.dto.AddParticipantRequest;
import com.coldchain.delivery.web.shipment.dto.CreateShipmentRequest;
import com.coldchain.delivery.web.shipment.dto.CustodyEventResponse;
import com.coldchain.delivery.web.shipment.dto.DispatchShipmentRequest;
import com.coldchain.delivery.web.shipment.dto.OpenHandoffRequest;
import com.coldchain.delivery.web.shipment.dto.OpenHandoffResponse;
import com.coldchain.delivery.web.shipment.dto.ShipmentLineResponse;
import com.coldchain.delivery.web.shipment.dto.ShipmentResponse;
import com.coldchain.delivery.web.shipment.dto.ShipmentTimelineResponse;
import com.coldchain.modules.shipment.api.dto.AcceptHandoffCommand;
import com.coldchain.modules.shipment.api.dto.AddParticipantCommand;
import com.coldchain.modules.shipment.api.dto.CreateShipmentCommand;
import com.coldchain.modules.shipment.api.dto.CustodyEventResult;
import com.coldchain.modules.shipment.api.dto.DispatchShipmentCommand;
import com.coldchain.modules.shipment.api.dto.OpenHandoffCommand;
import com.coldchain.modules.shipment.api.dto.OpenHandoffResult;
import com.coldchain.modules.shipment.api.dto.ShipmentLineCommand;
import com.coldchain.modules.shipment.api.dto.ShipmentLineResult;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.shipment.api.dto.ShipmentTimelineResult;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ShipmentWebMapper {

    public CreateShipmentCommand toCommand(UUID organizationId, CreateShipmentRequest request) {
        return new CreateShipmentCommand(organizationId, request.reference(), request.originSiteId(),
                request.destinationSiteId(), request.consigneeOrganizationId(),
                request.lines().stream()
                        .map(line -> new ShipmentLineCommand(line.productId(), line.quantity(),
                                line.unit()))
                        .toList());
    }

    public DispatchShipmentCommand toCommand(UUID shipmentId, DispatchShipmentRequest request) {
        return new DispatchShipmentCommand(shipmentId, request.deviceId());
    }

    public OpenHandoffCommand toCommand(UUID shipmentId, OpenHandoffRequest request) {
        return new OpenHandoffCommand(shipmentId, request.toOrganizationId());
    }

    public AcceptHandoffCommand toCommand(UUID shipmentId, UUID acceptingOrganizationId,
            AcceptHandoffRequest request) {
        return new AcceptHandoffCommand(shipmentId, acceptingOrganizationId, request.code());
    }

    public AddParticipantCommand toCommand(UUID shipmentId, AddParticipantRequest request) {
        return new AddParticipantCommand(shipmentId, request.participantOrganizationId(),
                request.participation());
    }

    public ShipmentResponse toResponse(ShipmentResult result) {
        return new ShipmentResponse(result.id(), result.reference(), result.status(),
                result.originSiteId(), result.destinationSiteId(), result.consigneeOrganizationId(),
                result.currentCustodianOrganizationId(), result.deviceId(), result.thresholds(),
                result.openExcursion(), result.dispatchedAt(), result.closedAt(),
                result.lines().stream().map(this::toResponse).toList());
    }

    public ShipmentLineResponse toResponse(ShipmentLineResult line) {
        return new ShipmentLineResponse(line.id(), line.productId(), line.productName(),
                line.profileVersion(), line.quantity(), line.unit());
    }

    public OpenHandoffResponse toResponse(OpenHandoffResult result) {
        return new OpenHandoffResponse(result.handoffId(), result.code(), result.expiresAt());
    }

    public ShipmentTimelineResponse toResponse(ShipmentTimelineResult result) {
        return new ShipmentTimelineResponse(result.shipmentId(),
                result.events().stream().map(this::toResponse).toList(), result.verdict());
    }

    public CustodyEventResponse toResponse(CustodyEventResult event) {
        return new CustodyEventResponse(event.sequenceNumber(), event.kind(),
                event.fromOrganizationId(), event.toOrganizationId(), event.siteId(),
                event.occurredAt(), event.recordedAt(), event.hash());
    }
}
