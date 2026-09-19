package com.coldchain.shared.util;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class RawUuid {

    private static final int RAW_LENGTH = 16;

    private RawUuid() {
    }

    public static byte[] toBytes(UUID value) {
        return ByteBuffer.allocate(RAW_LENGTH)
                .putLong(value.getMostSignificantBits())
                .putLong(value.getLeastSignificantBits())
                .array();
    }

    public static UUID fromBytes(byte[] value) {
        if (value.length != RAW_LENGTH) {
            throw new IllegalArgumentException(
                    "A RAW(16) identifier is exactly " + RAW_LENGTH + " bytes, received " + value.length);
        }
        ByteBuffer buffer = ByteBuffer.wrap(value);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
