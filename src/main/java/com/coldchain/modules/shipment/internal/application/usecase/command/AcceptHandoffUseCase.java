package com.coldchain.modules.shipment.internal.application.usecase.command;

import com.coldchain.modules.shipment.api.CustodyEventKind;
import com.coldchain.modules.shipment.api.Participation;
import com.coldchain.modules.shipment.api.dto.AcceptHandoffCommand;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.shipment.internal.application.CustodyLog;
import com.coldchain.modules.shipment.internal.application.mapper.ShipmentApiMapper;
import com.coldchain.modules.shipment.internal.domain.model.HandoffRequest;
import com.coldchain.modules.shipment.internal.domain.model.Shipment;
import com.coldchain.modules.shipment.internal.domain.model.ShipmentParticipant;
import com.coldchain.modules.shipment.internal.domain.repository.HandoffRequestRepository;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentParticipantRepository;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentRepository;
import com.coldchain.modules.shipment.internal.domain.service.HandoffCodeFactory;
import com.coldchain.modules.shipment.internal.exception.ShipmentErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import com.coldchain.shared.security.CurrentActor;
import java.time.Clock;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class AcceptHandoffUseCase {

    private final ShipmentRepository shipments;

    private final HandoffRequestRepository handoffs;

    private final ShipmentParticipantRepository participants;

    private final HandoffCodeFactory codes;

    private final CustodyLog custodyLog;

    private final ShipmentApiMapper mapper;

    private final CurrentActor currentActor;

    private final Clock clock;

    public AcceptHandoffUseCase(ShipmentRepository shipments, HandoffRequestRepository handoffs,
            ShipmentParticipantRepository participants, HandoffCodeFactory codes, CustodyLog custodyLog,
            ShipmentApiMapper mapper, CurrentActor currentActor, Clock clock) {
        this.shipments = shipments;
        this.handoffs = handoffs;
        this.participants = participants;
        this.codes = codes;
        this.custodyLog = custodyLog;
        this.mapper = mapper;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    @Transactional
    public ShipmentResult execute(AcceptHandoffCommand command) {
        Instant now = clock.instant();
        HandoffRequest pending = handoffs.findPending(command.shipmentId())
                .orElseThrow(() -> DomainException.of(ShipmentErrorCode.HANDOFF_NOT_FOUND));
        if (!pending.toOrganizationId().equals(command.acceptingOrganizationId())) {
            throw DomainException.of(ShipmentErrorCode.HANDOFF_NOT_YOURS);
        }
        if (pending.expiredAt(now)) {
            handoffs.save(pending.expire(now));
            throw DomainException.of(ShipmentErrorCode.HANDOFF_EXPIRED);
        }
        if (!pending.codeHash().equals(codes.fingerprint(command.code()))) {
            throw DomainException.of(ShipmentErrorCode.HANDOFF_CODE_INVALID);
        }
        Shipment shipment = shipments.findVisible(command.shipmentId(), pending.fromOrganizationId())
                .orElseThrow(() -> DomainException.of(ShipmentErrorCode.SHIPMENT_NOT_FOUND));
        handoffs.save(pending.accept(now));
        if (participants.findLive(shipment.id(), command.acceptingOrganizationId()).isEmpty()) {
            participants.save(ShipmentParticipant.createNew(shipment.id(),
                    command.acceptingOrganizationId(), Participation.CARRIER));
        }
        Shipment moved;
        try {
            moved = shipments.save(shipment.handOverTo(command.acceptingOrganizationId()));
        } catch (IllegalStateException refused) {
            throw DomainException.of(ShipmentErrorCode.ILLEGAL_TRANSITION, refused.getMessage());
        }
        custodyLog.append(moved.id(), CustodyEventKind.HANDOFF, pending.fromOrganizationId(),
                command.acceptingOrganizationId(), null, currentActor.requireId(), now);
        return mapper.toResult(moved);
    }
}
