package com.coldchain.modules.shipment.api.dto;

import com.coldchain.modules.shipment.api.Participation;
import java.util.UUID;

public record AddParticipantCommand(UUID shipmentId, UUID organizationId, Participation participation) {
}
