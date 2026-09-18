package com.coldchain.modules.shipment.internal.application;

import com.coldchain.modules.shipment.api.CustodyEventKind;
import com.coldchain.modules.shipment.internal.domain.model.CustodyEvent;
import com.coldchain.modules.shipment.internal.domain.repository.CustodyEventRepository;
import com.coldchain.modules.shipment.internal.domain.service.CustodyDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CustodyLog {

    private final CustodyEventRepository events;

    private final Clock clock;

    public CustodyLog(CustodyEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    public CustodyEvent append(UUID shipmentId, CustodyEventKind kind, UUID fromOrganizationId,
            UUID toOrganizationId, UUID siteId, UUID actorId, Instant rawOccurredAt) {
        Instant occurredAt = rawOccurredAt.truncatedTo(ChronoUnit.MICROS);
        CustodyEvent last = events.findLast(shipmentId).orElse(null);
        int sequenceNumber = last == null ? 1 : last.sequenceNumber() + 1;
        String previousHash = last == null ? CustodyEvent.GENESIS_HASH : last.hash();
        String hash = CustodyDigest.of(sequenceNumber, kind, fromOrganizationId, toOrganizationId,
                siteId, actorId, occurredAt, previousHash);
        return events.append(CustodyEvent.createNew(shipmentId, sequenceNumber, kind,
                fromOrganizationId, toOrganizationId, siteId, actorId, occurredAt,
                clock.instant().truncatedTo(ChronoUnit.MICROS), previousHash, hash));
    }
}
