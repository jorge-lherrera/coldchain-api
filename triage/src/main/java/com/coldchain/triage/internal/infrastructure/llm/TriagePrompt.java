package com.coldchain.triage.internal.infrastructure.llm;

import com.coldchain.triage.internal.domain.model.ContextHandoff;
import com.coldchain.triage.internal.domain.model.ContextReading;
import com.coldchain.triage.internal.domain.model.ExcursionAlert;
import com.coldchain.triage.internal.domain.model.ExcursionContext;
import com.coldchain.triage.internal.domain.model.FrozenThresholds;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.Resource;

/**
 * Fills the versioned prompt with measurements and the participation of the custody holder. Nothing
 * that identifies a shipment, an organization or a person goes in: the model sees what a sensor and a
 * custody log would show, and no more (ADR-012).
 */
final class TriagePrompt {

    static final String VERSION = "triage-v1";

    private static final String NOT_KNOWN = "not known";

    private final PromptTemplate template;

    TriagePrompt(Resource template) {
        this.template = new PromptTemplate(template);
    }

    Prompt of(ExcursionAlert alert, ExcursionContext context) {
        return template.create(Map.of(
                "kind", alert.kind().name(),
                "openedAt", orNotKnown(alert.openedAt(), Object::toString),
                "closedAt", orNotKnown(alert.closedAt(), Object::toString),
                "durationMinutes", orNotKnown(alert.durationMinutes(), Object::toString),
                "peak", orNotKnown(alert.peakCelsius(), peak -> celsius(peak) + " °C"),
                "thresholds", context.frozenThresholds().map(TriagePrompt::thresholds).orElse("- " + NOT_KNOWN),
                "custodian", context.custodian().name(),
                "minutesSinceLastHandoff", orNotKnown(context.minutesSinceLastHandoff(), Object::toString),
                "handoffs", lines(context.handoffs(), TriagePrompt::handoff),
                "readings", lines(context.readings(), TriagePrompt::reading)));
    }

    private static String thresholds(FrozenThresholds frozen) {
        return "- Range: " + celsius(frozen.minCelsius()) + " to " + celsius(frozen.maxCelsius()) + " °C\n"
                + "- A single excursion is allowed " + frozen.maxSingleExcursionMinutes() + " minutes\n"
                + "- All excursions together are allowed " + frozen.maxCumulativeExcursionMinutes() + " minutes";
    }

    private static String handoff(ContextHandoff handoff) {
        return handoff.occurredAt() + " from the " + handoff.from() + " to the " + handoff.to();
    }

    private static String reading(ContextReading reading) {
        return reading.measuredAt() + " " + celsius(reading.celsius());
    }

    private static <T> String lines(List<T> items, Function<T, String> line) {
        if (items.isEmpty()) {
            return "- none";
        }
        return items.stream().map(item -> "- " + line.apply(item)).collect(Collectors.joining("\n"));
    }

    private static <T> String orNotKnown(T value, Function<T, String> written) {
        return value == null ? NOT_KNOWN : written.apply(value);
    }

    private static String celsius(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
