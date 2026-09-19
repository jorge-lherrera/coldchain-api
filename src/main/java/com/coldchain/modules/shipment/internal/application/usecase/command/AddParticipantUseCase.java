package com.coldchain.modules.shipment.internal.application.usecase.command;

import com.coldchain.modules.shipment.api.Participation;
import com.coldchain.modules.shipment.api.dto.AddParticipantCommand;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.shipment.internal.application.mapper.ShipmentApiMapper;
import com.coldchain.modules.shipment.internal.domain.model.Shipment;
import com.coldchain.modules.shipment.internal.domain.model.ShipmentParticipant;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentParticipantRepository;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentRepository;
import com.coldchain.modules.shipment.internal.exception.ShipmentErrorCode;
import com.coldchain.shared.annotation.UseCase;
import com.coldchain.shared.exception.DomainException;
import com.coldchain.shared.security.CurrentActor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class AddParticipantUseCase {

    private final ShipmentRepository shipments;

    private final ShipmentParticipantRepository participants;

    private final ShipmentApiMapper mapper;

    private final CurrentActor currentActor;

    public AddParticipantUseCase(ShipmentRepository shipments,
            ShipmentParticipantRepository participants, ShipmentApiMapper mapper,
            CurrentActor currentActor) {
        this.shipments = shipments;
        this.participants = participants;
        this.mapper = mapper;
        this.currentActor = currentActor;
    }

    @Transactional
    public ShipmentResult execute(AddParticipantCommand command) {
        Shipment shipment = shipments
                .findVisible(command.shipmentId(), currentActor.requireOrganizationId())
                .orElseThrow(() -> DomainException.of(ShipmentErrorCode.SHIPMENT_NOT_FOUND));
        requireShipper(shipment);
        if (participants.findLive(shipment.id(), command.organizationId()).isPresent()) {
            throw DomainException.of(ShipmentErrorCode.PARTICIPANT_ALREADY_ADDED);
        }
        participants.save(ShipmentParticipant.createNew(shipment.id(), command.organizationId(),
                command.participation()));
        return mapper.toResult(shipment);
    }

    private void requireShipper(Shipment shipment) {
        boolean shipper = participants.findLive(shipment.id(), currentActor.requireOrganizationId())
                .filter(participant -> participant.participation() == Participation.SHIPPER)
                .isPresent();
        if (!shipper) {
            throw DomainException.of(ShipmentErrorCode.NOT_THE_SHIPPER);
        }
    }
}
