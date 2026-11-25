package com.coldchain.triage.internal.domain.model;

import java.time.Instant;
import java.util.Objects;

public record ContextHandoff(Instant occurredAt, CustodyParticipation from, CustodyParticipation to) {

    public ContextHandoff {
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
    }
}
