package com.coldchain.shared.config.infrastructure;

import java.time.Clock;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.stereotype.Component;

@Component("auditingDateTimeProvider")
public class AuditingDateTimeProvider implements DateTimeProvider {

    private final Clock clock;

    public AuditingDateTimeProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<TemporalAccessor> getNow() {
        return Optional.of(clock.instant());
    }
}
