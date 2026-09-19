package com.coldchain.modules.telemetry.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.telemetry.api.ExcursionKind;
import com.coldchain.modules.telemetry.api.ExcursionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExcursionTest {

    private static final UUID ORGANIZATION = UUID.fromString("01930000-0000-7000-8000-000000000001");

    private static final UUID SHIPMENT = UUID.fromString("01930000-0000-7000-8000-000000000002");

    private static final UUID FIRST_READING = UUID.fromString("01930000-0000-7000-8000-000000000003");

    private static final UUID LAST_READING = UUID.fromString("01930000-0000-7000-8000-000000000004");

    private static final Instant OPENED = Instant.parse("2026-04-01T09:00:00Z");

    @Test
    void anExcursionStartsOpenAtThePeakThatOpenedIt() {
        Excursion excursion = above(new BigDecimal("9.50"));

        assertThat(excursion.open()).isTrue();
        assertThat(excursion.status()).isEqualTo(ExcursionStatus.OPEN);
        assertThat(excursion.peakCelsius()).isEqualByComparingTo("9.50");
        assertThat(excursion.closedAt()).isNull();
        assertThat(excursion.durationMinutes())
                .describedAs("an excursion that has not ended has lasted nothing anybody can defend")
                .isZero();
    }

    @Test
    void theWorstReadingIsTheOneThatIsKeptAbove() {
        Excursion excursion = above(new BigDecimal("9.50"))
                .deepenedTo(new BigDecimal("11.50"), OPENED.plus(10, ChronoUnit.MINUTES))
                .deepenedTo(new BigDecimal("10.00"), OPENED.plus(20, ChronoUnit.MINUTES));

        assertThat(excursion.peakCelsius())
                .describedAs("the peak is how bad it got, not how bad it was last")
                .isEqualByComparingTo("11.50");
        assertThat(excursion.durationMinutes()).isEqualTo(20);
    }

    @Test
    void belowTheMinimumTheWorstReadingIsTheColdestOne() {
        Excursion excursion = Excursion.open(ORGANIZATION, SHIPMENT, ExcursionKind.BELOW_MIN, OPENED,
                        FIRST_READING, new BigDecimal("1.00"))
                .deepenedTo(new BigDecimal("-2.00"), OPENED.plus(10, ChronoUnit.MINUTES))
                .deepenedTo(new BigDecimal("0.50"), OPENED.plus(15, ChronoUnit.MINUTES));

        assertThat(excursion.peakCelsius())
                .describedAs("for a freezing excursion the worst reading is the lowest one")
                .isEqualByComparingTo("-2.00");
    }

    @Test
    void closingRecordsHowLongItLastedAndWhatEndedIt() {
        Excursion closed = above(new BigDecimal("9.50"))
                .deepenedTo(new BigDecimal("11.50"), OPENED.plus(30, ChronoUnit.MINUTES))
                .close(OPENED.plus(45, ChronoUnit.MINUTES), LAST_READING);

        assertThat(closed.open()).isFalse();
        assertThat(closed.status()).isEqualTo(ExcursionStatus.CLOSED);
        assertThat(closed.durationMinutes())
                .describedAs("the duration is measured from the first reading out of band to the "
                        + "one that came back, not between the readings in the middle")
                .isEqualTo(45);
        assertThat(closed.closedByReadingId()).isEqualTo(LAST_READING);
        assertThat(closed.openedByReadingId()).isEqualTo(FIRST_READING);
    }

    @Test
    void neitherDeepeningNorClosingTouchesTheExcursionItCameFrom() {
        Excursion open = above(new BigDecimal("9.50"));
        Excursion closed = open.close(OPENED.plus(10, ChronoUnit.MINUTES), LAST_READING);

        assertThat(open.open()).isTrue();
        assertThat(open.closedAt()).isNull();
        assertThat(closed.id())
                .describedAs("it is the same excursion, later")
                .isEqualTo(open.id());
    }

    private Excursion above(BigDecimal celsius) {
        return Excursion.open(ORGANIZATION, SHIPMENT, ExcursionKind.ABOVE_MAX, OPENED, FIRST_READING,
                celsius);
    }
}
