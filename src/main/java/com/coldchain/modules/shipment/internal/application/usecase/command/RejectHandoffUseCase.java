package com.coldchain.modules.shipment.internal.application.usecase.command;

import com.coldchain.modules.shipment.internal.domain.model.HandoffRequest;
import com.coldchain.modules.shipment.internal.domain.repository.HandoffRequestRepository;
import com.coldchain.modules.shipment.internal.exception.ShipmentErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RejectHandoffUseCase {

    private final HandoffRequestRepository handoffs;

    private final Clock clock;

    public RejectHandoffUseCase(HandoffRequestRepository handoffs, Clock clock) {
        this.handoffs = handoffs;
        this.clock = clock;
    }

    @Transactional
    public void execute(UUID shipmentId) {
        HandoffRequest pending = handoffs.findPending(shipmentId)
                .orElseThrow(() -> DomainException.of(ShipmentErrorCode.HANDOFF_NOT_FOUND));
        handoffs.save(pending.reject(clock.instant()));
    }
}
