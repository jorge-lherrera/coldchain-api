package com.coldchain.shared.identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RawUuidTest {

    @Test
    void theSixteenBytesAreTheCanonicalFormWithTheHyphensRemoved() {
        UUID identifier = UUID.fromString("0192f0a1-2b3c-7d4e-8f01-23456789abcd");
        assertThat(HexFormat.of().formatHex(RawUuid.toBytes(identifier)))
                .isEqualTo("0192f0a12b3c7d4e8f0123456789abcd");
    }

    @Test
    void aRoundTripReturnsTheSameIdentifier() {
        for (int i = 0; i < 1_000; i++) {
            UUID identifier = UuidV7.generate();
            assertThat(RawUuid.fromBytes(RawUuid.toBytes(identifier))).isEqualTo(identifier);
        }
    }

    @Test
    void anythingThatIsNotSixteenBytesIsRejectedInsteadOfSilentlyTruncated() {
        assertThatThrownBy(() -> RawUuid.fromBytes(new byte[15]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly 16 bytes");
    }
}
