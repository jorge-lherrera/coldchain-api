package com.coldchain.modules.shipment.internal.application.usecase.command;

import com.coldchain.modules.catalog.api.CatalogApi;
import com.coldchain.modules.catalog.api.dto.StorageProfileResult;
import com.coldchain.modules.shipment.api.CustodyEventKind;
import com.coldchain.modules.shipment.api.dto.DispatchShipmentCommand;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.shipment.api.event.ShipmentDispatched;
import com.coldchain.modules.shipment.internal.application.CustodyLog;
import com.coldchain.modules.shipment.internal.application.mapper.ShipmentApiMapper;
import com.coldchain.modules.shipment.internal.domain.model.FrozenThresholds;
import com.coldchain.modules.shipment.internal.domain.model.Shipment;
import com.coldchain.modules.shipment.internal.domain.model.ShipmentLine;
import com.coldchain.modules.shipment.internal.domain.repository.ShipmentRepository;
import com.coldchain.modules.shipment.internal.exception.ShipmentErrorCode;
import com.coldchain.modules.telemetry.api.TelemetryApi;
import com.coldchain.shared.annotation.UseCase;
import com.coldchain.shared.exception.DomainException;
import com.coldchain.shared.security.CurrentActor;
import java.time.Clock;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class DispatchShipmentUseCase {

    private final ShipmentRepository shipments;

    private final CatalogApi catalog;

    private final TelemetryApi telemetry;

    private final CustodyLog custodyLog;

    private final ShipmentApiMapper mapper;

    private final ApplicationEventPublisher events;

    private final CurrentActor currentActor;

    private final Clock clock;

    public DispatchShipmentUseCase(ShipmentRepository shipments, CatalogApi catalog,
            TelemetryApi telemetry, CustodyLog custodyLog, ShipmentApiMapper mapper,
            ApplicationEventPublisher events, CurrentActor currentActor, Clock clock) {
        this.shipments = shipments;
        this.catalog = catalog;
        this.telemetry = telemetry;
        this.custodyLog = custodyLog;
        this.mapper = mapper;
        this.events = events;
        this.currentActor = currentActor;
        this.clock = clock;
    }

    @Transactional
    public ShipmentResult execute(DispatchShipmentCommand command) {
        Shipment shipment = shipments
                .findVisible(command.shipmentId(), currentActor.requireOrganizationId())
                .orElseThrow(() -> DomainException.of(ShipmentErrorCode.SHIPMENT_NOT_FOUND));
        if (shipment.lines().isEmpty()) {
            throw DomainException.of(ShipmentErrorCode.NO_LINES);
        }
        if (command.deviceId() == null) {
            throw DomainException.of(ShipmentErrorCode.NO_DEVICE);
        }
        telemetry.deviceOf(command.deviceId());
        Instant now = clock.instant();
        Shipment dispatched;
        try {
            dispatched = shipments.save(
                    shipment.dispatch(command.deviceId(), freeze(shipment), now));
        } catch (IllegalStateException refused) {
            throw DomainException.of(ShipmentErrorCode.ILLEGAL_TRANSITION, refused.getMessage());
        }
        custodyLog.append(dispatched.id(), CustodyEventKind.DISPATCHED, null,
                dispatched.currentCustodianOrganizationId(), dispatched.originSiteId(),
                currentActor.requireId(), now);
        events.publishEvent(new ShipmentDispatched(dispatched.id(), dispatched.organizationId(),
                dispatched.deviceId(), mapper.toThresholds(dispatched.thresholds()), now));
        return mapper.toResult(dispatched);
    }

    private FrozenThresholds freeze(Shipment shipment) {
        ShipmentLine strictest = shipment.lines().getFirst();
        StorageProfileResult profile = catalog.activeProfileOf(strictest.productId());
        if (profile == null) {
            throw DomainException.of(ShipmentErrorCode.PROFILE_NOT_FROZEN);
        }
        return new FrozenThresholds(profile.thresholds().minCelsius(),
                profile.thresholds().maxCelsius(), profile.thresholds().maxSingleExcursionMinutes(),
                profile.thresholds().maxCumulativeExcursionMinutes(),
                profile.thresholds().minCoveragePercent());
    }
}
