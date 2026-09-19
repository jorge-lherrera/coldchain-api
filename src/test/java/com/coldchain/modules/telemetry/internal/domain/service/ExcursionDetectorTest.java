package com.coldchain.modules.telemetry.internal.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.telemetry.api.ExcursionKind;
import com.coldchain.modules.telemetry.internal.domain.model.TemperatureReading;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExcursionDetectorTest {

    private static final BigDecimal MINIMUM = new BigDecimal("2.00");

    private static final BigDecimal MAXIMUM = new BigDecimal("8.00");

    private static final UUID ORGANIZATION = UUID.fromString("01930000-0000-7000-8000-000000000001");

    private static final UUID DEVICE = UUID.fromString("01930000-0000-7000-8000-000000000002");

    private static final UUID SHIPMENT = UUID.fromString("01930000-0000-7000-8000-000000000003");

    private static final UUID BATCH = UUID.fromString("01930000-0000-7000-8000-000000000004");

    private static final Instant START = Instant.parse("2026-04-01T08:00:00Z");

    @Test
    void aJourneyInsideTheBandHasNothingToReport() {
        assertThat(detect(series("4.5", "5.0", "7.9", "2.0", "8.0"))).isEmpty();
    }

    @Test
    void theBoundariesThemselvesAreInsideTheBand() {
        assertThat(detect(series("2.00", "8.00")))
                .describedAs("a profile that promises 2 to 8 degrees promises those two as well")
                .isEmpty();
    }

    @Test
    void aSpikeOpensAndClosesWithTheReadingsThatSurroundIt() {
        List<DetectedExcursion> found = detect(series("4.5", "9.0", "11.5", "10.0", "4.5", "4.5"));

        assertThat(found).hasSize(1);
        DetectedExcursion excursion = found.getFirst();
        assertThat(excursion.kind()).isEqualTo(ExcursionKind.ABOVE_MAX);
        assertThat(excursion.closed()).isTrue();
        assertThat(excursion.openedAt()).isEqualTo(START.plus(5, ChronoUnit.MINUTES));
        assertThat(excursion.closedAt())
                .describedAs("the excursion ends when a reading comes back into the band, so the "
                        + "time out of band includes the gap that reading closes")
                .isEqualTo(START.plus(20, ChronoUnit.MINUTES));
        assertThat(excursion.peakCelsius()).isEqualByComparingTo("11.5");
    }

    @Test
    void anExcursionStillRunningAtTheEndIsReportedOpen() {
        List<DetectedExcursion> found = detect(series("4.5", "9.0", "9.5"));

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().closed())
                .describedAs("a series that ends out of band is not a series that came back")
                .isFalse();
        assertThat(found.getFirst().closedAt()).isNull();
        assertThat(found.getFirst().lastSeenAt()).isEqualTo(START.plus(10, ChronoUnit.MINUTES));
    }

    @Test
    void twoSpikesWithAReturnBetweenThemAreTwoExcursions() {
        List<DetectedExcursion> found =
                detect(series("4.5", "9.0", "4.5", "9.5", "4.5"));

        assertThat(found).hasSize(2);
        assertThat(found).allSatisfy(excursion -> assertThat(excursion.closed()).isTrue());
    }

    @Test
    void goingStraightFromTooHotToTooColdIsTwoExcursionsAndNotOne() {
        List<DetectedExcursion> found = detect(series("4.5", "9.0", "1.0", "4.5"));

        assertThat(found)
                .describedAs("the two breaches damage the product in opposite ways and cannot be "
                        + "reported as one span")
                .hasSize(2);
        assertThat(found.get(0).kind()).isEqualTo(ExcursionKind.ABOVE_MAX);
        assertThat(found.get(1).kind()).isEqualTo(ExcursionKind.BELOW_MIN);
    }

    @Test
    void theColdestReadingIsThePeakOfAFreezingExcursion() {
        List<DetectedExcursion> found = detect(series("4.5", "1.0", "-3.0", "0.5", "4.5"));

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().kind()).isEqualTo(ExcursionKind.BELOW_MIN);
        assertThat(found.getFirst().peakCelsius()).isEqualByComparingTo("-3.0");
    }

    @Test
    void readingsThatArriveOutOfOrderAreJudgedInTheOrderTheyWereMeasured() {
        List<TemperatureReading> series = series("4.5", "9.0", "11.5", "4.5");
        List<TemperatureReading> shuffled = new ArrayList<>(series);
        Collections.reverse(shuffled);

        assertThat(detect(shuffled))
                .describedAs("a gateway that uploads a batch backwards must not invent a different "
                        + "journey from the same readings")
                .isEqualTo(detect(series));
    }

    private List<DetectedExcursion> detect(List<TemperatureReading> series) {
        return ExcursionDetector.detect(series, MINIMUM, MAXIMUM);
    }

    private List<TemperatureReading> series(String... celsius) {
        List<TemperatureReading> readings = new ArrayList<>();
        for (int index = 0; index < celsius.length; index++) {
            readings.add(TemperatureReading.createNew(ORGANIZATION, DEVICE, SHIPMENT, BATCH,
                    new BigDecimal(celsius[index]), START.plus(index * 5L, ChronoUnit.MINUTES)));
        }
        return readings;
    }
}
