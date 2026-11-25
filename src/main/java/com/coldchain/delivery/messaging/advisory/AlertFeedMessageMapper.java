package com.coldchain.delivery.messaging.advisory;

import com.coldchain.modules.advisory.api.AlertSeverity;
import com.coldchain.modules.advisory.api.AlertSource;
import com.coldchain.modules.advisory.api.ProbableCause;
import com.coldchain.modules.advisory.api.dto.RecordAlertCommand;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the alert as triage writes it, with no type shared between the two services (ADR-010). A
 * message that can never become a command is unreadable, and the error handler dead-letters that
 * without a retry, because a retry would read the same bytes the same way.
 */
@Component
public class AlertFeedMessageMapper {

    private final JsonMapper json;

    public AlertFeedMessageMapper(JsonMapper json) {
        this.json = json;
    }

    public RecordAlertCommand toCommand(String payload) {
        AlertFeedMessage message = read(payload);
        requirePresent(message);
        return new RecordAlertCommand(message.excursionId(), message.shipmentId(), message.organizationId(),
                oneOf(ProbableCause.values(), message.probableCause(), "cause"),
                oneOf(AlertSeverity.values(), message.severity(), "severity"), message.confidence(),
                message.summaryEn(), message.summaryEs(), message.summaryPt(), message.recommendedAction(),
                oneOf(AlertSource.values(), message.source(), "source"), message.model(),
                message.contextAvailable(), message.triagedAt());
    }

    private AlertFeedMessage read(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new UnreadableAlertMessageException("An empty message");
        }
        try {
            return json.readValue(payload, AlertFeedMessage.class);
        } catch (JacksonException cause) {
            throw new UnreadableAlertMessageException("Not an alert: " + cause.getOriginalMessage(), cause);
        }
    }

    private static void requirePresent(AlertFeedMessage message) {
        if (message.excursionId() == null || message.shipmentId() == null || message.organizationId() == null
                || message.contextAvailable() == null || message.triagedAt() == null) {
            throw new UnreadableAlertMessageException("An alert without one of its required fields");
        }
    }

    private static <E extends Enum<E>> E oneOf(E[] allowed, String value, String what) {
        Optional<E> found = Arrays.stream(allowed).filter(candidate -> candidate.name().equals(value)).findFirst();
        return found.orElseThrow(() -> new UnreadableAlertMessageException("An alert with an unknown " + what));
    }
}
