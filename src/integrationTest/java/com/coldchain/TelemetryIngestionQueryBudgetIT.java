package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.telemetry.api.TelemetryApi;
import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.api.dto.IngestBatchCommand;
import com.coldchain.modules.telemetry.api.dto.IngestBatchResult;
import com.coldchain.modules.telemetry.api.dto.ReadingCommand;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@IntegrationTest
@Import(QueryCounter.class)
class TelemetryIngestionQueryBudgetIT {

    private static final int READINGS = 200;

    private static final int BUDGET = 9;

    @Autowired
    private TelemetryApi telemetry;

    @Autowired
    private Fixtures fixtures;

    @Test
    void twoHundredReadingsAreOneInsertAndNotTwoHundred() {
        RegisterOrganizationResult owner = fixtures.organization("ingest");
        DeviceResult device = fixtures.device(owner);
        ShipmentResult shipment = fixtures.dispatchedShipment(owner, device);

        QueryCounter.reset();
        IngestBatchResult accepted = telemetry.ingestBatch(new IngestBatchCommand(
                owner.organizationId(), device.id(), "batch-" + UUID.randomUUID(),
                samples(shipment)));
        int spent = QueryCounter.counted();

        assertThat(accepted.acceptedCount()).isEqualTo(READINGS);
        assertThat(spent)
                .describedAs("the identifiers are generated in the application precisely so the "
                        + "whole batch is one statement instead of one round trip per row")
                .isEqualTo(BUDGET);
    }

    private List<ReadingCommand> samples(ShipmentResult shipment) {
        List<ReadingCommand> readings = new ArrayList<>();
        for (int minute = 0; minute < READINGS; minute++) {
            readings.add(new ReadingCommand(shipment.dispatchedAt().plus(minute, ChronoUnit.SECONDS),
                    new BigDecimal("4.50")));
        }
        return readings;
    }
}
