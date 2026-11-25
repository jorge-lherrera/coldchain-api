package com.coldchain.modules.shipment.internal.domain.model;

import com.coldchain.modules.shipment.api.Participation;
import java.time.Instant;
import java.util.Objects;

public record CustodyHandoff(Instant occurredAt, Participation from, Participation to) {

    public CustodyHandoff {
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
    }
}
