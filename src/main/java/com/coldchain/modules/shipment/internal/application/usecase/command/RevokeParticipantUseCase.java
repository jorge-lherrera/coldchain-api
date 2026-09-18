package com.coldchain.modules.shipment.internal.application.usecase.command;

import com.coldchain.modules.shipment.api.Participation;
import com.coldchain.modules.shipment.internal.domain.model.Shipment;
import com.coldchain.modules.shipment.internal.domain.model.ShipmentParticipant;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentParticipantRepository;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentRepository;
import com.coldchain.modules.shipment.internal.exception.ShipmentErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import com.coldchain.shared.security.CurrentActor;
import java.time.Clock;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RevokeParticipantUseCase {

    private final ShipmentRepository shipments;

    private final ShipmentParticipantRepository participants;

    private final CurrentActor currentActor;

    private final Clock clock;

    public RevokeParticipantUseCase(ShipmentRepository shipments,
            ShipmentParticipantRepository participants, CurrentActor currentActor, Clock clock) {
        this.shipments = shipments;
        this.participants = participants;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    @Transactional
    public void execute(UUID shipmentId, UUID organizationId) {
        Shipment shipment = shipments.findVisible(shipmentId, currentActor.requireOrganizationId())
                .orElseThrow(() -> DomainException.of(ShipmentErrorCode.SHIPMENT_NOT_FOUND));
        boolean shipper = participants.findLive(shipment.id(), currentActor.requireOrganizationId())
                .filter(participant -> participant.participation() == Participation.SHIPPER)
                .isPresent();
        if (!shipper) {
            throw DomainException.of(ShipmentErrorCode.NOT_THE_SHIPPER);
        }
        ShipmentParticipant participant = participants.findLive(shipment.id(), organizationId)
                .orElseThrow(() -> DomainException.of(ShipmentErrorCode.PARTICIPANT_NOT_FOUND));
        participants.save(participant.revoke(clock.instant()));
    }
}
