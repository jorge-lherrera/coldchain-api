package com.coldchain.triage.internal.application.usecase.command;

import com.coldchain.triage.internal.application.mapper.AlertTriagedMapper;
import com.coldchain.triage.internal.domain.model.ExcursionAlert;
import com.coldchain.triage.internal.domain.model.ExcursionContext;
import com.coldchain.triage.internal.domain.model.Triage;
import com.coldchain.triage.internal.domain.repository.ExcursionAlertRepository;
import com.coldchain.triage.internal.domain.repository.InboxEventRepository;
import com.coldchain.triage.internal.domain.service.ExcursionContextSource;
import com.coldchain.triage.internal.domain.service.RuleBasedTriage;
import com.coldchain.triage.internal.domain.service.TriageAdvisor;
import com.coldchain.triage.shared.annotation.UseCase;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Triages a closed excursion between two transactions, so that neither the API nor the model is
 * waited on with a connection held. The model is asked only when the API gave the context: without
 * the readings there is nothing for it to read, and the rules say as much.
 */
@UseCase
public class TriageExcursionUseCase {

    private final ExcursionAlertRepository alerts;

    private final InboxEventRepository inbox;

    private final ExcursionContextSource contexts;

    private final TriageAdvisor advisor;

    private final TransactionOperations transactions;

    private final ApplicationEventPublisher events;

    private final Clock clock;

    public TriageExcursionUseCase(ExcursionAlertRepository alerts, InboxEventRepository inbox,
            ExcursionContextSource contexts, TriageAdvisor advisor, TransactionOperations transactions,
            ApplicationEventPublisher events, Clock clock) {
        this.alerts = alerts;
        this.inbox = inbox;
        this.contexts = contexts;
        this.advisor = advisor;
        this.transactions = transactions;
        this.events = events;
        this.clock = clock;
    }

    public void execute(UUID eventId, UUID excursionId) {
        ExcursionAlert recorded = alertOf(excursionId);
        Optional<ExcursionContext> context = contexts.contextOf(recorded.shipmentId(), excursionId);
        Optional<Triage> advised = context.flatMap(found -> advisor.adviseOn(recorded, found));
        transactions.executeWithoutResult(status -> {
            Instant now = clock.instant();
            ExcursionAlert alert = alertOf(excursionId);
            Triage triage = advised.orElseGet(() -> context
                    .map(found -> RuleBasedTriage.of(alert, found, now))
                    .orElseGet(() -> RuleBasedTriage.of(alert, now)));
            ExcursionAlert triaged = alerts.save(alert.triagedBy(triage));
            if (triaged.triage() == triage) {
                events.publishEvent(AlertTriagedMapper.toEvent(triaged));
            }
            inbox.findById(eventId)
                    .filter(received -> !received.processed())
                    .ifPresent(received -> inbox.save(received.processedAt(now)));
        });
    }

    private ExcursionAlert alertOf(UUID excursionId) {
        return alerts.findByExcursionId(excursionId).orElseThrow(() -> new IllegalStateException(
                "Excursion " + excursionId + " was recorded and is gone before its triage"));
    }
}
