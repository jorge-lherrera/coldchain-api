package com.coldchain.modules.shipment.internal.application.usecase.query;

import com.coldchain.modules.shipment.api.dto.ExcursionContextResult;
import com.coldchain.modules.shipment.internal.application.mapper.ShipmentApiMapper;
import com.coldchain.modules.shipment.internal.domain.model.CustodyHistory;
import com.coldchain.modules.shipment.internal.domain.model.Shipment;
import com.coldchain.modules.shipment.internal.domain.repository.CustodyEventRepository;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentParticipantRepository;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentRepository;
import com.coldchain.modules.shipment.internal.exception.ShipmentErrorCode;
import com.coldchain.modules.telemetry.api.TelemetryApi;
import com.coldchain.modules.telemetry.api.dto.ExcursionWindowResult;
import com.coldchain.shared.annotation.UseCase;
import com.coldchain.shared.exception.DomainException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@UseCase
public class ExcursionContextUseCase {

    private static final Duration MARGIN = Duration.ofMinutes(30);

    private final ShipmentRepository shipments;

    private final CustodyEventRepository events;

    private final ShipmentParticipantRepository participants;

    private final TelemetryApi telemetry;

    private final ShipmentApiMapper mapper;

    public ExcursionContextUseCase(ShipmentRepository shipments, CustodyEventRepository events,
            ShipmentParticipantRepository participants, TelemetryApi telemetry,
            ShipmentApiMapper mapper) {
        this.shipments = shipments;
        this.events = events;
        this.participants = participants;
        this.telemetry = telemetry;
        this.mapper = mapper;
    }

    public ExcursionContextResult execute(UUID shipmentId, UUID excursionId) {
        Shipment shipment = shipments.findForPlatform(shipmentId)
                .orElseThrow(() -> DomainException.of(ShipmentErrorCode.SHIPMENT_NOT_FOUND));
        ExcursionWindowResult window = telemetry.excursionWindowOf(shipmentId, excursionId, MARGIN);
        CustodyHistory custody = CustodyHistory.restore(events.findOrderedBySequence(shipmentId),
                participants.findEveryOf(shipmentId));
        Instant openedAt = window.excursion().openedAt();
        return new ExcursionContextResult(shipmentId, window.excursion(), window.from(), window.to(),
                window.readings(), mapper.toThresholds(shipment.thresholds()),
                custody.holderAt(openedAt),
                custody.sinceLastHandoffAt(openedAt).map(Duration::toMinutes).orElse(null),
                custody.handoffsBetween(window.from(), window.to()).stream()
                        .map(mapper::toChange)
                        .toList());
    }
}
