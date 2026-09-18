package com.coldchain.modules.compliance.internal.infrastructure.messaging;

import com.coldchain.modules.compliance.internal.application.usecase.command.IssueCertificateUseCase;
import com.coldchain.modules.shipment.api.ShipmentStatus;
import com.coldchain.modules.shipment.api.event.ShipmentClosed;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ShipmentClosedListener {

    private final IssueCertificateUseCase issueCertificate;

    public ShipmentClosedListener(IssueCertificateUseCase issueCertificate) {
        this.issueCertificate = issueCertificate;
    }

    @EventListener
    public void certifyWhatTheJourneyProved(ShipmentClosed closed) {
        if (closed.status() == ShipmentStatus.CANCELLED) {
            return;
        }
        issueCertificate.execute(closed.shipmentId(), closed.organizationId());
    }
}
