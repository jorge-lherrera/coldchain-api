package com.coldchain.shared.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    private static final int BURST = 200_000;

    @Test
    void everyIdentifierDeclaresVersionSevenAndTheRfcVariant() {
        for (int i = 0; i < 1_000; i++) {
            UUID identifier = UuidV7.generate();
            assertThat(identifier.version()).isEqualTo(7);
            assertThat(identifier.variant()).isEqualTo(2);
        }
    }

    @Test
    void aBurstGeneratedFasterThanTheClockTicksStillOnlyEverGoesUp() {
        UUID previous = UuidV7.generate();
        for (int i = 1; i < BURST; i++) {
            UUID current = UuidV7.generate();
            assertThat(asUnsignedComparison(previous, current))
                    .describedAs("identifier %s followed %s", current, previous)
                    .isNegative();
            previous = current;
        }
    }

    @Test
    void theBytesThatReachTheIndexSortTheSameWayTheIdentifiersDo() {
        UUID first = UuidV7.generate();
        UUID second = UuidV7.generate();
        byte[] firstBytes = RawUuid.toBytes(first);
        byte[] secondBytes = RawUuid.toBytes(second);
        assertThat(java.util.Arrays.compareUnsigned(firstBytes, secondBytes)).isNegative();
    }

    @Test
    void concurrentGeneratorsNeverHandOutTheSameIdentifier() throws Exception {
        int threads = 16;
        int perThread = 20_000;
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Callable<List<UUID>>> jobs = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                jobs.add(() -> {
                    List<UUID> generated = new ArrayList<>(perThread);
                    for (int j = 0; j < perThread; j++) {
                        generated.add(UuidV7.generate());
                    }
                    return generated;
                });
            }
            Set<UUID> all = new HashSet<>(threads * perThread);
            for (Future<List<UUID>> job : pool.invokeAll(jobs)) {
                all.addAll(job.get());
            }
            assertThat(all).hasSize(threads * perThread);
        }
    }

    @Test
    void theEmbeddedInstantIsTheMomentOfCreation() {
        Instant before = Instant.now();
        UUID identifier = UuidV7.generate();
        Instant after = Instant.now();
        Instant embedded = UuidV7.creationInstantOf(identifier);
        assertThat(embedded).isBetween(before.minusMillis(1), after.plus(Duration.ofSeconds(1)));
    }

    @Test
    void readingTheInstantOfSomethingThatIsNotVersionSevenIsRejected() {
        UUID random = UUID.randomUUID();
        assertThatThrownBy(() -> UuidV7.creationInstantOf(random))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version 7");
    }

    private static int asUnsignedComparison(UUID left, UUID right) {
        return java.util.Arrays.compareUnsigned(RawUuid.toBytes(left), RawUuid.toBytes(right));
    }
}
