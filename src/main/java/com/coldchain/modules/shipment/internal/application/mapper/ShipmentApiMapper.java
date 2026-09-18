package com.coldchain.modules.shipment.internal.application.mapper;

import com.coldchain.modules.shipment.api.dto.CustodyEventResult;
import com.coldchain.modules.shipment.api.dto.ShipmentLineResult;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.shipment.api.dto.ShipmentThresholds;
import com.coldchain.modules.shipment.internal.domain.model.CustodyEvent;
import com.coldchain.modules.shipment.internal.domain.model.FrozenThresholds;
import com.coldchain.modules.shipment.internal.domain.model.Shipment;
import com.coldchain.modules.shipment.internal.domain.model.ShipmentLine;
import org.springframework.stereotype.Component;

@Component
public class ShipmentApiMapper {

    public ShipmentResult toResult(Shipment shipment) {
        return new ShipmentResult(shipment.id(), shipment.organizationId(), shipment.reference(),
                shipment.status(), shipment.originSiteId(), shipment.destinationSiteId(),
                shipment.consigneeOrganizationId(), shipment.currentCustodianOrganizationId(),
                shipment.deviceId(), toThresholds(shipment.thresholds()), shipment.openExcursion(),
                shipment.dispatchedAt(), shipment.closedAt(),
                shipment.lines().stream().map(this::toResult).toList());
    }

    public ShipmentLineResult toResult(ShipmentLine line) {
        return new ShipmentLineResult(line.id(), line.productId(), line.productName(),
                line.profileVersion(), line.quantity(), line.unit());
    }

    public CustodyEventResult toResult(CustodyEvent event) {
        return new CustodyEventResult(event.id(), event.sequenceNumber(), event.kind(),
                event.fromOrganizationId(), event.toOrganizationId(), event.siteId(), event.actorId(),
                event.occurredAt(), event.recordedAt(), event.previousHash(), event.hash());
    }

    public ShipmentThresholds toThresholds(FrozenThresholds thresholds) {
        if (thresholds == null) {
            return null;
        }
        return new ShipmentThresholds(thresholds.minCelsius(), thresholds.maxCelsius(),
                thresholds.maxSingleExcursionMinutes(), thresholds.maxCumulativeExcursionMinutes(),
                thresholds.minCoveragePercent());
    }
}
