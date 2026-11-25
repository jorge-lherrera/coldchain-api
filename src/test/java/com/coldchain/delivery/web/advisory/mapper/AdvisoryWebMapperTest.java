package com.coldchain.delivery.web.advisory.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.delivery.web.advisory.dto.ExcursionAlertResponse;
import com.coldchain.modules.advisory.api.AlertSeverity;
import com.coldchain.modules.advisory.api.AlertSource;
import com.coldchain.modules.advisory.api.ProbableCause;
import com.coldchain.modules.advisory.api.dto.ExcursionAlertResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.i18n.LocaleContextHolder;

class AdvisoryWebMapperTest {

    private static final ExcursionAlertResult ALERT = new ExcursionAlertResult(UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), ProbableCause.DOOR_OPENING, AlertSeverity.LOW, new BigDecimal("0.80"),
            "A short spike.", "Un pico corto.", "Um pico curto.", "Keep the doors shut.", AlertSource.LLM,
            "claude-opus-5-5", true, Instant.parse("2026-05-01T10:50:00Z"));

    private final AdvisoryWebMapper mapper = new AdvisoryWebMapper();

    @AfterEach
    void forgetTheLanguage() {
        LocaleContextHolder.resetLocaleContext();
    }

    @ParameterizedTest
    @CsvSource({"en, A short spike.", "es, Un pico corto.", "pt, Um pico curto.", "de, A short spike."})
    void theSummaryIsTheOneInTheLanguageAskedForAndEnglishOtherwise(String language, String expected) {
        LocaleContextHolder.setLocale(Locale.forLanguageTag(language));

        List<ExcursionAlertResponse> responses = mapper.toResponses(List.of(ALERT));

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.summary()).isEqualTo(expected);
            assertThat(response.probableCause()).isEqualTo(ProbableCause.DOOR_OPENING);
            assertThat(response.confidence())
                    .describedAs("a cause never travels without how sure the triage was")
                    .isEqualByComparingTo("0.80");
            assertThat(response.source()).isEqualTo(AlertSource.LLM);
        });
    }
}
