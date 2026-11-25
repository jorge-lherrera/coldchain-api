package com.coldchain.modules.shipment.api.dto;

import com.coldchain.modules.shipment.api.Participation;
import com.coldchain.modules.telemetry.api.dto.ExcursionResult;
import com.coldchain.modules.telemetry.api.dto.SeriesPoint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExcursionContextResult(
        UUID shipmentId,
        ExcursionResult excursion,
        Instant windowFrom,
        Instant windowTo,
        List<SeriesPoint> readings,
        ShipmentThresholds thresholds,
        Participation custodian,
        Long minutesSinceLastHandoff,
        List<CustodyChange> handoffs) {
}
