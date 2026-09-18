package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import com.coldchain.modules.telemetry.api.TelemetryApi;
import com.coldchain.modules.telemetry.api.dto.DeviceResult;
import com.coldchain.modules.telemetry.api.dto.IngestBatchCommand;
import com.coldchain.modules.telemetry.api.dto.ReadingCommand;
import com.coldchain.modules.telemetry.api.dto.SeriesResult;
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
class TelemetrySeriesQueryBudgetIT {

    private static final int BUDGET = 2;

    @Autowired
    private TelemetryApi telemetry;

    @Autowired
    private Fixtures fixtures;

    @Test
    void readingThirtySamplesCostsTheSameAsReadingOne() {
        int spentOnFive = spentReadingSeries("five", 5);
        int spentOnThirty = spentReadingSeries("thirty", 30);

        assertThat(spentOnThirty)
                .describedAs("the cost of reading the series grew with the number of samples")
                .isEqualTo(spentOnFive);
        assertThat(spentOnThirty)
                .describedAs("the series is one read of the readings and one of the excursions, "
                        + "and it stays that way however many samples there are")
                .isEqualTo(BUDGET);
    }

    private int spentReadingSeries(String name, int howMany) {
        RegisterOrganizationResult owner = fixtures.organization(name);
        DeviceResult device = fixtures.device(owner);
        ShipmentResult shipment = fixtures.dispatchedShipment(owner, device);
        telemetry.ingestBatch(new IngestBatchCommand(owner.organizationId(), device.id(),
                "batch-" + UUID.randomUUID(), samples(shipment, howMany)));

        QueryCounter.reset();
        SeriesResult series = telemetry.seriesOf(shipment.id(), owner.organizationId());
        int spent = QueryCounter.counted();

        assertThat(series.points()).hasSize(howMany);
        return spent;
    }

    private List<ReadingCommand> samples(ShipmentResult shipment, int howMany) {
        List<ReadingCommand> readings = new ArrayList<>();
        for (int minute = 0; minute < howMany; minute++) {
            readings.add(new ReadingCommand(shipment.dispatchedAt().plus(minute, ChronoUnit.MINUTES),
                    new BigDecimal("4.50")));
        }
        return readings;
    }
}
