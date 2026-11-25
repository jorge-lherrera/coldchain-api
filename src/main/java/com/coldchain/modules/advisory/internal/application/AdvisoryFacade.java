package com.coldchain.modules.advisory.internal.application;

import com.coldchain.modules.advisory.api.AdvisoryApi;
import com.coldchain.modules.advisory.api.dto.ExcursionAlertResult;
import com.coldchain.modules.advisory.api.dto.RecordAlertCommand;
import com.coldchain.modules.advisory.internal.application.usecase.command.RecordAlertUseCase;
import com.coldchain.modules.advisory.internal.application.usecase.query.ListShipmentAlertsUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AdvisoryFacade implements AdvisoryApi {

    private final RecordAlertUseCase recordAlert;

    private final ListShipmentAlertsUseCase listShipmentAlerts;

    public AdvisoryFacade(RecordAlertUseCase recordAlert, ListShipmentAlertsUseCase listShipmentAlerts) {
        this.recordAlert = recordAlert;
        this.listShipmentAlerts = listShipmentAlerts;
    }

    @Override
    public ExcursionAlertResult recordAlert(RecordAlertCommand command) {
        return recordAlert.execute(command);
    }

    @Override
    public List<ExcursionAlertResult> alertsOf(UUID shipmentId, UUID viewerOrganizationId) {
        return listShipmentAlerts.execute(shipmentId, viewerOrganizationId);
    }
}
