package com.coldchain.modules.advisory.internal.application.usecase.query;

import com.coldchain.modules.advisory.api.dto.ExcursionAlertResult;
import com.coldchain.modules.advisory.internal.application.mapper.AdvisoryApiMapper;
import com.coldchain.modules.advisory.internal.domain.repository.ExcursionAlertRepository;
import com.coldchain.modules.shipment.api.ShipmentApi;
import com.coldchain.shared.annotation.UseCase;
import java.util.List;
import java.util.UUID;

@UseCase
public class ListShipmentAlertsUseCase {

    private final ExcursionAlertRepository alerts;

    private final ShipmentApi shipments;

    private final AdvisoryApiMapper mapper;

    public ListShipmentAlertsUseCase(ExcursionAlertRepository alerts, ShipmentApi shipments,
            AdvisoryApiMapper mapper) {
        this.alerts = alerts;
        this.shipments = shipments;
        this.mapper = mapper;
    }

    public List<ExcursionAlertResult> execute(UUID shipmentId, UUID viewerOrganizationId) {
        shipments.shipmentOf(shipmentId, viewerOrganizationId);
        return alerts.findOfShipment(shipmentId).stream().map(mapper::toResult).toList();
    }
}
