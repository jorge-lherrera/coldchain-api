package com.coldchain.delivery.web.shipment.dto;

import com.coldchain.modules.shipment.api.Participation;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddParticipantRequest(@NotNull UUID participantOrganizationId,
        @NotNull Participation participation) {
}
