package com.coldchain.modules.shipment.internal.application.usecase.command;

import com.coldchain.modules.shipment.api.dto.OpenHandoffCommand;
import com.coldchain.modules.shipment.api.dto.OpenHandoffResult;
import com.coldchain.modules.shipment.internal.domain.model.HandoffRequest;
import com.coldchain.modules.shipment.internal.domain.model.Shipment;
import com.coldchain.modules.shipment.internal.domain.repository.HandoffRequestRepository;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentRepository;
import com.coldchain.modules.shipment.internal.domain.service.HandoffCodeFactory;
import com.coldchain.modules.shipment.internal.domain.service.IssuedCode;
import com.coldchain.modules.shipment.internal.exception.ShipmentErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import com.coldchain.shared.security.CurrentActor;
import java.time.Clock;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class OpenHandoffUseCase {

    private final ShipmentRepository shipments;

    private final HandoffRequestRepository handoffs;

    private final HandoffCodeFactory codes;

    private final CurrentActor currentActor;

    private final Clock clock;

    public OpenHandoffUseCase(ShipmentRepository shipments, HandoffRequestRepository handoffs,
            HandoffCodeFactory codes, CurrentActor currentActor, Clock clock) {
        this.shipments = shipments;
        this.handoffs = handoffs;
        this.codes = codes;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    @Transactional
    public OpenHandoffResult execute(OpenHandoffCommand command) {
        Shipment shipment = shipments
                .findVisible(command.shipmentId(), currentActor.requireOrganizationId())
                .orElseThrow(() -> DomainException.of(ShipmentErrorCode.SHIPMENT_NOT_FOUND));
        if (!shipment.currentCustodianOrganizationId().equals(currentActor.requireOrganizationId())) {
            throw DomainException.of(ShipmentErrorCode.NOT_THE_CUSTODIAN);
        }
        IssuedCode code = codes.issue();
        Instant expiresAt = clock.instant().plus(codes.lifetime());
        HandoffRequest opened = handoffs.save(HandoffRequest.open(shipment.id(),
                shipment.currentCustodianOrganizationId(), command.toOrganizationId(),
                code.fingerprint(), expiresAt));
        return new OpenHandoffResult(opened.id(), code.plainCode(), expiresAt);
    }
}
