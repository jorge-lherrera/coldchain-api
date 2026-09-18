package com.coldchain.modules.shipment.internal.application.usecase.command;

import com.coldchain.modules.shipment.api.CustodyEventKind;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.shipment.internal.application.CustodyLog;
import com.coldchain.modules.shipment.internal.application.mapper.ShipmentApiMapper;
import com.coldchain.modules.shipment.internal.domain.model.Shipment;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentRepository;
import com.coldchain.modules.shipment.internal.exception.ShipmentErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import com.coldchain.shared.security.CurrentActor;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class ArriveShipmentUseCase {

    private final ShipmentRepository shipments;

    private final CustodyLog custodyLog;

    private final ShipmentApiMapper mapper;

    private final CurrentActor currentActor;

    private final Clock clock;

    public ArriveShipmentUseCase(ShipmentRepository shipments, CustodyLog custodyLog, ShipmentApiMapper mapper,
            CurrentActor currentActor, Clock clock) {
        this.shipments = shipments;
        this.custodyLog = custodyLog;
        this.mapper = mapper;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    @Transactional
    public ShipmentResult execute(UUID shipmentId) {
        Shipment shipment = shipments.findVisible(shipmentId, currentActor.requireOrganizationId())
                .orElseThrow(() -> DomainException.of(ShipmentErrorCode.SHIPMENT_NOT_FOUND));
        Instant now = clock.instant();
        Shipment moved;
        try {
            moved = shipments.save(shipment.arrive());
        } catch (IllegalStateException refused) {
            throw DomainException.of(ShipmentErrorCode.ILLEGAL_TRANSITION, refused.getMessage());
        }
        custodyLog.append(moved.id(), CustodyEventKind.ARRIVED, null,
                moved.currentCustodianOrganizationId(), moved.destinationSiteId(),
                currentActor.requireId(), now);
        return mapper.toResult(moved);
    }
}
