package com.coldchain.modules.shipment.internal.infrastructure.messaging;

import com.coldchain.modules.shipment.internal.domain.repository.ShipmentRepository;
import com.coldchain.modules.telemetry.api.event.ExcursionClosed;
import com.coldchain.modules.telemetry.api.event.ExcursionOpened;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ExcursionListener {

    private final ShipmentRepository shipments;

    public ExcursionListener(ShipmentRepository shipments) {
        this.shipments = shipments;
    }

    @EventListener
    public void on(ExcursionOpened event) {
        shipments.markOpenExcursion(event.shipmentId(), true);
    }

    @EventListener
    public void on(ExcursionClosed event) {
        shipments.markOpenExcursion(event.shipmentId(), false);
    }
}
