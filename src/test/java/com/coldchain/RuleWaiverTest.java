package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coldchain.RuleWaivers.Waiver;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RuleWaiverTest {

    private static final List<Waiver> REGISTER = List.of(
            new Waiver("R9.1", "closed by default", "LegacyController; PublicPingController", "jorge",
                    "2030-01-01"));

    @Test
    void everyWaiverNamesAnOwnerAndAnExpiryDate() {
        assertThat(RuleWaivers.waivers()).allSatisfy(waiver -> {
            assertThat(waiver.owner()).describedAs("%s has no owner", waiver.ruleId()).isNotBlank();
            assertThat(waiver.expiresOn()).describedAs("%s has no usable expiry", waiver.ruleId()).isNotNull();
        });
    }

    @Test
    void noWaiverHasExpiredBecauseAnExpiredWaiverIsAViolation() {
        assertThat(RuleWaivers.waivers())
                .filteredOn(waiver -> waiver.expiresOn().isBefore(LocalDate.now()))
                .isEmpty();
    }

    @Test
    void theRegisterIsReadFromTheCatalogueColumns() {
        List<Waiver> parsed = RuleWaivers.waivers(List.of(
                "## Waiver register",
                "| Id | Rule | What is tolerated | Owner | Expires |",
                "|---|---|---|---|---|",
                "| R9.1 | closed by default | LegacyController; PublicPingController | jorge | 2030-01-01 |"));

        assertThat(parsed).singleElement().satisfies(waiver -> {
            assertThat(waiver.ruleId()).isEqualTo("R9.1");
            assertThat(waiver.owner()).isEqualTo("jorge");
            assertThat(waiver.expiresOn()).isEqualTo(LocalDate.of(2030, 1, 1));
            assertThat(waiver.toleratedViolations())
                    .containsExactly("LegacyController", "PublicPingController");
        });
    }

    @Test
    void anExactMatchBetweenWhatIsFoundAndWhatIsToleratedPasses() {
        assertThatCode(() -> RuleWaivers.assertOnlyWaived("R9.1",
                Set.of("LegacyController", "PublicPingController"), REGISTER))
                .doesNotThrowAnyException();
    }

    @Test
    void aViolationThatIsNotOnTheListFailsBecauseDebtDoesNotGrow() {
        assertThatThrownBy(() -> RuleWaivers.assertOnlyWaived("R9.1",
                Set.of("LegacyController", "PublicPingController", "NewController"), REGISTER))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("New violations: NewController");
    }

    @Test
    void aViolationThatDisappearedAlsoFailsBecauseGroundGainedIsNotGivenBack() {
        assertThatThrownBy(() -> RuleWaivers.assertOnlyWaived("R9.1", Set.of("LegacyController"), REGISTER))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Waivers to remove: PublicPingController");
    }
}
