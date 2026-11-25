package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.advisory.api.AdvisoryApi;
import com.coldchain.modules.advisory.api.AlertSeverity;
import com.coldchain.modules.advisory.api.AlertSource;
import com.coldchain.modules.advisory.api.ProbableCause;
import com.coldchain.modules.advisory.api.dto.ExcursionAlertResult;
import com.coldchain.modules.advisory.api.dto.RecordAlertCommand;
import com.coldchain.modules.identity.api.dto.RegisterOrganizationResult;
import com.coldchain.modules.shipment.api.dto.ShipmentResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@IntegrationTest
@Import(QueryCounter.class)
class ShipmentAlertsQueryBudgetIT {

    private static final int BUDGET = 3;

    @Autowired
    private AdvisoryApi advisory;

    @Autowired
    private Fixtures fixtures;

    @Test
    void readingTenAlertsCostsTheSameAsReadingOne() {
        int spentOnOne = spentReadingAlerts("one", 1);
        int spentOnTen = spentReadingAlerts("ten", 10);

        assertThat(spentOnTen)
                .describedAs("the cost of reading the alerts grew with the number of alerts")
                .isEqualTo(spentOnOne);
        assertThat(spentOnTen)
                .describedAs("the alerts cost what seeing the shipment costs plus one read of the alerts, "
                        + "however many there are")
                .isEqualTo(BUDGET);
    }

    private int spentReadingAlerts(String name, int howMany) {
        RegisterOrganizationResult owner = fixtures.organization(name);
        ShipmentResult shipment = fixtures.draftShipment(owner);
        Instant first = Instant.parse("2026-05-01T10:00:00Z");
        for (int alert = 0; alert < howMany; alert++) {
            advisory.recordAlert(new RecordAlertCommand(UUID.randomUUID(), shipment.id(), owner.organizationId(),
                    ProbableCause.UNKNOWN, AlertSeverity.MEDIUM, null, "Out of range.", "Fuera de rango.",
                    "Fora da faixa.", "Check the shipment now.", AlertSource.RULES, null, false,
                    first.plus(alert, ChronoUnit.MINUTES)));
        }

        QueryCounter.reset();
        List<ExcursionAlertResult> alerts = advisory.alertsOf(shipment.id(), owner.organizationId());
        int spent = QueryCounter.counted();

        assertThat(alerts).hasSize(howMany);
        return spent;
    }
}
