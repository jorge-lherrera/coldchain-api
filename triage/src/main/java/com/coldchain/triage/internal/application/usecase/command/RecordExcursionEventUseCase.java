package com.coldchain.triage.internal.application.usecase.command;

import com.coldchain.triage.api.EventOutcome;
import com.coldchain.triage.api.dto.RecordExcursionEventCommand;
import com.coldchain.triage.api.dto.RecordExcursionEventResult;
import com.coldchain.triage.internal.domain.model.ExcursionAlert;
import com.coldchain.triage.internal.domain.model.ExcursionEvent;
import com.coldchain.triage.internal.domain.model.InboxEvent;
import com.coldchain.triage.internal.domain.repository.ExcursionAlertRepository;
import com.coldchain.triage.internal.domain.repository.InboxEventRepository;
import com.coldchain.triage.internal.domain.service.RuleBasedTriage;
import com.coldchain.triage.shared.annotation.UseCase;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RecordExcursionEventUseCase {

    private final InboxEventRepository inbox;

    private final ExcursionAlertRepository alerts;

    private final Clock clock;

    public RecordExcursionEventUseCase(InboxEventRepository inbox, ExcursionAlertRepository alerts, Clock clock) {
        this.inbox = inbox;
        this.alerts = alerts;
        this.clock = clock;
    }

    @Transactional
    public RecordExcursionEventResult execute(RecordExcursionEventCommand command) {
        ExcursionEvent event = new ExcursionEvent(command.eventId(), command.type(), command.occurredAt(),
                command.shipmentId(), command.organizationId(), command.excursionId(), command.kind(),
                command.peakCelsius(), command.durationMinutes());
        Optional<InboxEvent> seen = inbox.findById(event.eventId());
        if (seen.map(InboxEvent::processed).orElse(false)) {
            return new RecordExcursionEventResult(event.eventId(), event.excursionId(), EventOutcome.DUPLICATE,
                    false);
        }

        Instant now = clock.instant();
        ExcursionAlert alert = alerts.findByExcursionId(event.excursionId())
                .map(stored -> stored.apply(event))
                .orElseGet(() -> ExcursionAlert.firstHeardFrom(event));
        InboxEvent received = seen.orElseGet(() -> InboxEvent.received(event, now));
        EventOutcome outcome = seen.isPresent() ? EventOutcome.RESUMED : EventOutcome.RECORDED;
        if (alert.closed()) {
            alerts.save(alert);
            inbox.save(received);
            return new RecordExcursionEventResult(event.eventId(), event.excursionId(), outcome, true);
        }
        alerts.save(alert.triagedBy(RuleBasedTriage.of(alert, now)));
        inbox.save(received.processedAt(now));
        return new RecordExcursionEventResult(event.eventId(), event.excursionId(), outcome, false);
    }
}
