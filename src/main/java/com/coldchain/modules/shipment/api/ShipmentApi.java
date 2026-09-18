package com.coldchain.modules.shipment.api;

import com.coldchain.modules.shipment.api.dto.AcceptHandoffCommand;
import com.coldchain.modules.shipment.api.dto.AddParticipantCommand;
import com.coldchain.modules.shipment.api.dto.CreateShipmentCommand;
import com.coldchain.modules.shipment.api.dto.DispatchShipmentCommand;
import com.coldchain.modules.shipment.api.dto.OpenHandoffCommand;
import com.coldchain.modules.shipment.api.dto.OpenHandoffResult;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.shipment.api.dto.ShipmentTimelineResult;
import com.coldchain.shared.paging.PageCriteria;
import com.coldchain.shared.paging.PagedResult;
import java.util.UUID;

public interface ShipmentApi {

    ShipmentResult createShipment(CreateShipmentCommand command);

    ShipmentResult dispatchShipment(DispatchShipmentCommand command);

    ShipmentResult cancelShipment(UUID shipmentId);

    ShipmentResult arriveShipment(UUID shipmentId);

    ShipmentResult deliverShipment(UUID shipmentId);

    ShipmentResult rejectShipment(UUID shipmentId);

    OpenHandoffResult openHandoff(OpenHandoffCommand command);

    ShipmentResult acceptHandoff(AcceptHandoffCommand command);

    void rejectHandoff(UUID shipmentId);

    ShipmentResult addParticipant(AddParticipantCommand command);

    void revokeParticipant(UUID shipmentId, UUID organizationId);

    ShipmentResult shipmentOf(UUID shipmentId, UUID viewerOrganizationId);

    ShipmentTimelineResult timelineOf(UUID shipmentId, UUID viewerOrganizationId);

    PagedResult<ShipmentResult> listShipments(UUID viewerOrganizationId, PageCriteria criteria);
}
