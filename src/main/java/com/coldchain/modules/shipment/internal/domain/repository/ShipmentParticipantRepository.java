package com.coldchain.modules.shipment.internal.domain.repository;

import com.coldchain.modules.shipment.internal.domain.model.ShipmentParticipant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentParticipantRepository {

    ShipmentParticipant save(ShipmentParticipant participant);

    List<ShipmentParticipant> findLiveOf(UUID shipmentId);

    Optional<ShipmentParticipant> findLive(UUID shipmentId, UUID organizationId);
}
