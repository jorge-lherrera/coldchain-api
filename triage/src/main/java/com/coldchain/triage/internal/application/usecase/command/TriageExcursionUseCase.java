package com.coldchain.triage.internal.application.usecase.command;

import com.coldchain.triage.internal.domain.model.ExcursionAlert;
import com.coldchain.triage.internal.domain.model.ExcursionContext;
import com.coldchain.triage.internal.domain.model.Triage;
import com.coldchain.triage.internal.domain.repository.ExcursionAlertRepository;
import com.coldchain.triage.internal.domain.repository.InboxEventRepository;
import com.coldchain.triage.internal.domain.service.ExcursionContextSource;
import com.coldchain.triage.internal.domain.service.RuleBasedTriage;
import com.coldchain.triage.shared.annotation.UseCase;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.support.TransactionOperations;

@UseCase
public class TriageExcursionUseCase {

    private final ExcursionAlertRepository alerts;

    private final InboxEventRepository inbox;

    private final ExcursionContextSource contexts;

    private final TransactionOperations transactions;

    private final Clock clock;

    public TriageExcursionUseCase(ExcursionAlertRepository alerts, InboxEventRepository inbox,
            ExcursionContextSource contexts, TransactionOperations transactions, Clock clock) {
        this.alerts = alerts;
        this.inbox = inbox;
        this.contexts = contexts;
        this.transactions = transactions;
        this.clock = clock;
    }

    public void execute(UUID eventId, UUID excursionId) {
        ExcursionAlert recorded = alertOf(excursionId);
        Optional<ExcursionContext> context = contexts.contextOf(recorded.shipmentId(), excursionId);
        transactions.executeWithoutResult(status -> {
            Instant now = clock.instant();
            ExcursionAlert alert = alertOf(excursionId);
            Triage triage = context
                    .map(found -> RuleBasedTriage.of(alert, found, now))
                    .orElseGet(() -> RuleBasedTriage.of(alert, now));
            alerts.save(alert.triagedBy(triage));
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
