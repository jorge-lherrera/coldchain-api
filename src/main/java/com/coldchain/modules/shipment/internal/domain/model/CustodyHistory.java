package com.coldchain.modules.shipment.internal.domain.model;

import com.coldchain.modules.shipment.api.CustodyEventKind;
import com.coldchain.modules.shipment.api.Participation;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CustodyHistory {

    private final List<CustodyEvent> chain;

    private final Map<UUID, Participation> participations;

    private CustodyHistory(List<CustodyEvent> chain, Map<UUID, Participation> participations) {
        if (chain.isEmpty()) {
            throw new IllegalArgumentException("A shipment with no custody log has no history to read");
        }
        this.chain = List.copyOf(chain);
        this.participations = Map.copyOf(participations);
    }

    public static CustodyHistory restore(List<CustodyEvent> chain,
            List<ShipmentParticipant> participants) {
        Map<UUID, Participation> participations = new HashMap<>();
        participants.stream()
                .sorted(Comparator.comparing(participant -> participant.revokedAt() == null))
                .forEach(participant -> participations.put(participant.organizationId(),
                        participant.participation()));
        return new CustodyHistory(chain.stream()
                .sorted(Comparator.comparingInt(CustodyEvent::sequenceNumber))
                .toList(), participations);
    }

    public Participation holderAt(Instant moment) {
        UUID holder = chain.stream()
                .filter(event -> !event.occurredAt().isAfter(moment))
                .reduce((earlier, later) -> later)
                .orElse(chain.getFirst())
                .toOrganizationId();
        return participationOf(holder);
    }

    public Optional<Duration> sinceLastHandoffAt(Instant moment) {
        return chain.stream()
                .filter(event -> event.kind() == CustodyEventKind.HANDOFF)
                .filter(event -> !event.occurredAt().isAfter(moment))
                .reduce((earlier, later) -> later)
                .map(handoff -> Duration.between(handoff.occurredAt(), moment));
    }

    public List<CustodyHandoff> handoffsBetween(Instant from, Instant to) {
        return chain.stream()
                .filter(event -> event.kind() == CustodyEventKind.HANDOFF)
                .filter(event -> !event.occurredAt().isBefore(from) && !event.occurredAt().isAfter(to))
                .map(handoff -> new CustodyHandoff(handoff.occurredAt(),
                        participationOf(handoff.fromOrganizationId()),
                        participationOf(handoff.toOrganizationId())))
                .toList();
    }

    private Participation participationOf(UUID organizationId) {
        Participation participation = participations.get(organizationId);
        if (participation == null) {
            throw new IllegalStateException(
                    "An organization held custody without ever taking part in the shipment");
        }
        return participation;
    }
}
