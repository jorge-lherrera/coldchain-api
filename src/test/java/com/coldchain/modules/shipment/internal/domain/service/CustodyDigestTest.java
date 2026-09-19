package com.coldchain.modules.shipment.internal.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.modules.shipment.api.CustodyEventKind;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustodyDigestTest {

    private static final UUID SHIPPER = UUID.fromString("01930000-0000-7000-8000-000000000001");

    private static final UUID CARRIER = UUID.fromString("01930000-0000-7000-8000-000000000002");

    private static final UUID SITE = UUID.fromString("01930000-0000-7000-8000-000000000003");

    private static final UUID ACTOR = UUID.fromString("01930000-0000-7000-8000-000000000004");

    private static final Instant WHEN = Instant.parse("2026-04-01T08:00:00Z");

    private static final String GENESIS = "0".repeat(64);

    @Test
    void theSameEventAlwaysHashesTheSameWay() {
        assertThat(hash(1, GENESIS))
                .describedAs("a chain that cannot be recomputed cannot be verified by anybody else")
                .isEqualTo(hash(1, GENESIS))
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }

    @Test
    void everyPartOfTheEventChangesTheHash() {
        String original = hash(1, GENESIS);

        assertThat(CustodyDigest.of(2, CustodyEventKind.DISPATCHED, SHIPPER, CARRIER, SITE, ACTOR,
                WHEN, GENESIS)).isNotEqualTo(original);
        assertThat(CustodyDigest.of(1, CustodyEventKind.DELIVERED, SHIPPER, CARRIER, SITE, ACTOR,
                WHEN, GENESIS)).isNotEqualTo(original);
        assertThat(CustodyDigest.of(1, CustodyEventKind.DISPATCHED, CARRIER, CARRIER, SITE, ACTOR,
                WHEN, GENESIS)).isNotEqualTo(original);
        assertThat(CustodyDigest.of(1, CustodyEventKind.DISPATCHED, SHIPPER, SHIPPER, SITE, ACTOR,
                WHEN, GENESIS)).isNotEqualTo(original);
        assertThat(CustodyDigest.of(1, CustodyEventKind.DISPATCHED, SHIPPER, CARRIER, ACTOR, ACTOR,
                WHEN, GENESIS)).isNotEqualTo(original);
        assertThat(CustodyDigest.of(1, CustodyEventKind.DISPATCHED, SHIPPER, CARRIER, SITE, SHIPPER,
                WHEN, GENESIS)).isNotEqualTo(original);
        assertThat(CustodyDigest.of(1, CustodyEventKind.DISPATCHED, SHIPPER, CARRIER, SITE, ACTOR,
                WHEN.plusSeconds(1), GENESIS))
                .describedAs("moving an event one second changes what the chain says happened")
                .isNotEqualTo(original);
    }

    @Test
    void rewritingALinkBreaksEverythingAfterIt() {
        String first = hash(1, GENESIS);
        String second = hash(2, first);
        String third = hash(3, second);

        String tamperedFirst = CustodyDigest.of(1, CustodyEventKind.DISPATCHED, SHIPPER, CARRIER,
                SITE, ACTOR, WHEN.plusSeconds(60), GENESIS);
        String secondAfterTampering = hash(2, tamperedFirst);

        assertThat(secondAfterTampering)
                .describedAs("editing an event without editing its successors leaves a chain that "
                        + "no longer verifies, which is the whole point of chaining it")
                .isNotEqualTo(second);
        assertThat(hash(3, secondAfterTampering)).isNotEqualTo(third);
    }

    @Test
    void anEventWithNoCounterpartyHashesWithoutOne() {
        assertThat(CustodyDigest.of(1, CustodyEventKind.ARRIVED, null, null, null, ACTOR, WHEN,
                GENESIS))
                .describedAs("an arrival hands the box to nobody, and a missing party is a value "
                        + "like any other, not a reason to fail")
                .hasSize(64);
    }

    private String hash(int sequenceNumber, String previousHash) {
        return CustodyDigest.of(sequenceNumber, CustodyEventKind.DISPATCHED, SHIPPER, CARRIER, SITE,
                ACTOR, WHEN, previousHash);
    }
}
