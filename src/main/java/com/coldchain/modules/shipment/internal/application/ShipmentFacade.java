package com.coldchain.modules.shipment.internal.application;

import com.coldchain.modules.shipment.api.ShipmentApi;
import com.coldchain.modules.shipment.api.dto.AcceptHandoffCommand;
import com.coldchain.modules.shipment.api.dto.AddParticipantCommand;
import com.coldchain.modules.shipment.api.dto.CreateShipmentCommand;
import com.coldchain.modules.shipment.api.dto.DispatchShipmentCommand;
import com.coldchain.modules.shipment.api.dto.OpenHandoffCommand;
import com.coldchain.modules.shipment.api.dto.OpenHandoffResult;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.shipment.api.dto.ShipmentTimelineResult;
import com.coldchain.modules.shipment.internal.application.usecase.command.AcceptHandoffUseCase;
import com.coldchain.modules.shipment.internal.application.usecase.command.AddParticipantUseCase;
import com.coldchain.modules.shipment.internal.application.usecase.command.ArriveShipmentUseCase;
import com.coldchain.modules.shipment.internal.application.usecase.command.CancelShipmentUseCase;
import com.coldchain.modules.shipment.internal.application.usecase.command.CreateShipmentUseCase;
import com.coldchain.modules.shipment.internal.application.usecase.command.DeliverShipmentUseCase;
import com.coldchain.modules.shipment.internal.application.usecase.command.DispatchShipmentUseCase;
import com.coldchain.modules.shipment.internal.application.usecase.command.OpenHandoffUseCase;
import com.coldchain.modules.shipment.internal.application.usecase.command.RejectHandoffUseCase;
import com.coldchain.modules.shipment.internal.application.usecase.command.RejectShipmentUseCase;
import com.coldchain.modules.shipment.internal.application.usecase.command.RevokeParticipantUseCase;
import com.coldchain.modules.shipment.internal.application.usecase.query.GetShipmentUseCase;
import com.coldchain.modules.shipment.internal.application.usecase.query.ListShipmentsUseCase;
import com.coldchain.modules.shipment.internal.application.usecase.query.ShipmentTimelineUseCase;
import com.coldchain.shared.pagination.PageCriteria;
import com.coldchain.shared.pagination.PagedResult;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ShipmentFacade implements ShipmentApi {

    private final CreateShipmentUseCase createShipment;

    private final DispatchShipmentUseCase dispatchShipment;

    private final CancelShipmentUseCase cancelShipment;

    private final ArriveShipmentUseCase arriveShipment;

    private final DeliverShipmentUseCase deliverShipment;

    private final RejectShipmentUseCase rejectShipment;

    private final OpenHandoffUseCase openHandoff;

    private final AcceptHandoffUseCase acceptHandoff;

    private final RejectHandoffUseCase rejectHandoff;

    private final AddParticipantUseCase addParticipant;

    private final RevokeParticipantUseCase revokeParticipant;

    private final GetShipmentUseCase getShipment;

    private final ShipmentTimelineUseCase timeline;

    private final ListShipmentsUseCase listShipments;

    public ShipmentFacade(CreateShipmentUseCase createShipment,
            DispatchShipmentUseCase dispatchShipment, CancelShipmentUseCase cancelShipment,
            ArriveShipmentUseCase arriveShipment, DeliverShipmentUseCase deliverShipment,
            RejectShipmentUseCase rejectShipment, OpenHandoffUseCase openHandoff,
            AcceptHandoffUseCase acceptHandoff, RejectHandoffUseCase rejectHandoff,
            AddParticipantUseCase addParticipant, RevokeParticipantUseCase revokeParticipant,
            GetShipmentUseCase getShipment, ShipmentTimelineUseCase timeline,
            ListShipmentsUseCase listShipments) {
        this.createShipment = createShipment;
        this.dispatchShipment = dispatchShipment;
        this.cancelShipment = cancelShipment;
        this.arriveShipment = arriveShipment;
        this.deliverShipment = deliverShipment;
        this.rejectShipment = rejectShipment;
        this.openHandoff = openHandoff;
        this.acceptHandoff = acceptHandoff;
        this.rejectHandoff = rejectHandoff;
        this.addParticipant = addParticipant;
        this.revokeParticipant = revokeParticipant;
        this.getShipment = getShipment;
        this.timeline = timeline;
        this.listShipments = listShipments;
    }

    @Override
    public ShipmentResult createShipment(CreateShipmentCommand command) {
        return createShipment.execute(command);
    }

    @Override
    public ShipmentResult dispatchShipment(DispatchShipmentCommand command) {
        return dispatchShipment.execute(command);
    }

    @Override
    public ShipmentResult cancelShipment(UUID shipmentId) {
        return cancelShipment.execute(shipmentId);
    }

    @Override
    public ShipmentResult arriveShipment(UUID shipmentId) {
        return arriveShipment.execute(shipmentId);
    }

    @Override
    public ShipmentResult deliverShipment(UUID shipmentId) {
        return deliverShipment.execute(shipmentId);
    }

    @Override
    public ShipmentResult rejectShipment(UUID shipmentId) {
        return rejectShipment.execute(shipmentId);
    }

    @Override
    public OpenHandoffResult openHandoff(OpenHandoffCommand command) {
        return openHandoff.execute(command);
    }

    @Override
    public ShipmentResult acceptHandoff(AcceptHandoffCommand command) {
        return acceptHandoff.execute(command);
    }

    @Override
    public void rejectHandoff(UUID shipmentId) {
        rejectHandoff.execute(shipmentId);
    }

    @Override
    public ShipmentResult addParticipant(AddParticipantCommand command) {
        return addParticipant.execute(command);
    }

    @Override
    public void revokeParticipant(UUID shipmentId, UUID organizationId) {
        revokeParticipant.execute(shipmentId, organizationId);
    }

    @Override
    public ShipmentResult shipmentOf(UUID shipmentId, UUID viewerOrganizationId) {
        return getShipment.execute(shipmentId, viewerOrganizationId);
    }

    @Override
    public ShipmentTimelineResult timelineOf(UUID shipmentId, UUID viewerOrganizationId) {
        return timeline.execute(shipmentId, viewerOrganizationId);
    }

    @Override
    public PagedResult<ShipmentResult> listShipments(UUID viewerOrganizationId,
            PageCriteria criteria) {
        return listShipments.execute(viewerOrganizationId, criteria);
    }
}
