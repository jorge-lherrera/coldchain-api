package com.coldchain.modules.catalog.internal.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ThresholdsTest {

    @Test
    void aFridgeProfileIsAcceptedAsWritten() {
        Thresholds fridge = new Thresholds(new BigDecimal("2.00"), new BigDecimal("8.00"), 30, 120,
                new BigDecimal("80.00"));

        assertThat(fridge.minCelsius()).isEqualByComparingTo("2.00");
        assertThat(fridge.maxCelsius()).isEqualByComparingTo("8.00");
    }

    @Test
    void aFreezerProfileIsAcceptedToo() {
        Thresholds freezer = new Thresholds(new BigDecimal("-25.00"), new BigDecimal("-15.00"), 0, 0,
                new BigDecimal("95.00"));

        assertThat(freezer.maxSingleExcursionMinutes())
                .describedAs("a profile that tolerates nothing is a profile, not an error")
                .isZero();
    }

    @Test
    void aRangeUpsideDownIsRefused() {
        assertThatThrownBy(() -> new Thresholds(new BigDecimal("8.00"), new BigDecimal("2.00"), 30,
                120, new BigDecimal("80.00")))
                .describedAs("a band nothing can be inside would fail every journey ever measured "
                        + "against it")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aRangeWithNoWidthIsRefused() {
        assertThatThrownBy(() -> new Thresholds(new BigDecimal("5.00"), new BigDecimal("5.00"), 30,
                120, new BigDecimal("80.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anAllowanceLargerThanTheTotalIsRefused() {
        assertThatThrownBy(() -> new Thresholds(new BigDecimal("2.00"), new BigDecimal("8.00"), 200,
                120, new BigDecimal("80.00")))
                .describedAs("one excursion allowed to run longer than everything allowed together "
                        + "makes the total meaningless")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anAllowanceBelowZeroIsRefused() {
        assertThatThrownBy(() -> new Thresholds(new BigDecimal("2.00"), new BigDecimal("8.00"), -1,
                120, new BigDecimal("80.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void coverageOutsideAPercentageIsRefused() {
        assertThatThrownBy(() -> new Thresholds(new BigDecimal("2.00"), new BigDecimal("8.00"), 30,
                120, new BigDecimal("101.00")))
                .describedAs("asking for more coverage than exists is a requirement nothing can meet")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Thresholds(new BigDecimal("2.00"), new BigDecimal("8.00"), 30,
                120, new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aProfileWithoutItsNumbersIsRefused() {
        assertThatThrownBy(() -> new Thresholds(null, new BigDecimal("8.00"), 30, 120,
                new BigDecimal("80.00")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Thresholds(new BigDecimal("2.00"), new BigDecimal("8.00"), 30,
                120, null))
                .isInstanceOf(NullPointerException.class);
    }
}
