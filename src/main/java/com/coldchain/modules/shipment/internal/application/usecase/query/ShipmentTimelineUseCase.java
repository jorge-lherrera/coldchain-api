package com.coldchain.modules.shipment.internal.application.usecase.query;

import com.coldchain.modules.shipment.api.dto.ChainVerdict;
import com.coldchain.modules.shipment.api.dto.CustodyEventResult;
import com.coldchain.modules.shipment.api.dto.ShipmentTimelineResult;
import com.coldchain.modules.shipment.internal.application.mapper.ShipmentApiMapper;
import com.coldchain.modules.shipment.internal.domain.model.CustodyEvent;
import com.coldchain.modules.shipment.internal.domain.repository.CustodyEventRepository;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentRepository;
import com.coldchain.modules.shipment.internal.domain.service.CustodyDigest;
import com.coldchain.modules.shipment.internal.exception.ShipmentErrorCode;
import com.coldchain.shared.application.UseCase;
import com.coldchain.shared.error.DomainException;
import java.util.List;
import java.util.UUID;

@UseCase
public class ShipmentTimelineUseCase {

    private final ShipmentRepository shipments;

    private final CustodyEventRepository events;

    private final ShipmentApiMapper mapper;

    public ShipmentTimelineUseCase(ShipmentRepository shipments, CustodyEventRepository events,
            ShipmentApiMapper mapper) {
        this.shipments = shipments;
        this.events = events;
        this.mapper = mapper;
    }

    public ShipmentTimelineResult execute(UUID shipmentId, UUID viewerOrganizationId) {
        shipments.findVisible(shipmentId, viewerOrganizationId)
                .orElseThrow(() -> DomainException.of(ShipmentErrorCode.SHIPMENT_NOT_FOUND));
        List<CustodyEvent> chain = events.findOrderedBySequence(shipmentId);
        List<CustodyEventResult> results = chain.stream().map(mapper::toResult).toList();
        return new ShipmentTimelineResult(shipmentId, results, verify(chain));
    }

    private ChainVerdict verify(List<CustodyEvent> chain) {
        String expectedPrevious = CustodyEvent.GENESIS_HASH;
        for (CustodyEvent event : chain) {
            if (!event.previousHash().equals(expectedPrevious)) {
                return new ChainVerdict(false, event.sequenceNumber(),
                        "The event does not chain against the one before it");
            }
            String recomputed = CustodyDigest.of(event.sequenceNumber(), event.kind(),
                    event.fromOrganizationId(), event.toOrganizationId(), event.siteId(),
                    event.actorId(), event.occurredAt(), event.previousHash());
            if (!recomputed.equals(event.hash())) {
                return new ChainVerdict(false, event.sequenceNumber(),
                        "The stored hash does not match the event it claims to cover");
            }
            expectedPrevious = event.hash();
        }
        return new ChainVerdict(true, null, "Every event chains against the one before it");
    }
}
