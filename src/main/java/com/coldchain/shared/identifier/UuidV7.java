package com.coldchain.shared.identifier;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class UuidV7 {

    private static final int COUNTER_BITS = 12;

    private static final long COUNTER_MASK = (1L << COUNTER_BITS) - 1;

    private static final int TIMESTAMP_SHIFT = 16;

    private static final long VERSION = 0x7000L;

    private static final long VARIANT = 0x8000000000000000L;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final AtomicLong STATE = new AtomicLong();

    private UuidV7() {
    }

    public static UUID generate() {
        long state = nextState();
        long timestamp = state >>> COUNTER_BITS;
        long counter = state & COUNTER_MASK;
        long mostSignificantBits = (timestamp << TIMESTAMP_SHIFT) | VERSION | counter;
        long leastSignificantBits = (RANDOM.nextLong() >>> 2) | VARIANT;
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    public static Instant creationInstantOf(UUID value) {
        if (value.version() != 7) {
            throw new IllegalArgumentException("Not a version 7 identifier: " + value);
        }
        return Instant.ofEpochMilli(value.getMostSignificantBits() >>> TIMESTAMP_SHIFT);
    }

    private static long nextState() {
        while (true) {
            long previous = STATE.get();
            long next = advance(previous, System.currentTimeMillis());
            if (STATE.compareAndSet(previous, next)) {
                return next;
            }
        }
    }

    private static long advance(long previous, long now) {
        long previousMilliseconds = previous >>> COUNTER_BITS;
        if (now > previousMilliseconds) {
            return now << COUNTER_BITS;
        }
        if ((previous & COUNTER_MASK) < COUNTER_MASK) {
            return previous + 1;
        }
        return (previousMilliseconds + 1) << COUNTER_BITS;
    }
}
