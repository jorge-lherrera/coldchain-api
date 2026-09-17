package com.coldchain.shared.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProblemTypeTest {

    @Test
    void theUriIsDerivedFromTheMessageKeyAndNeverWrittenByHand() {
        assertThat(ProblemType.from("error.shipment.handoff_expired"))
                .hasToString("urn:coldchain:problem:shipment:handoff-expired");
    }

    @Test
    void everyCoreCodeProducesAUsableType() {
        for (CoreErrorCode code : CoreErrorCode.values()) {
            assertThat(ProblemType.from(code.messageKey()).toString())
                    .startsWith("urn:coldchain:problem:")
                    .doesNotContain("_");
        }
    }

    @Test
    void aKeyThatDoesNotLookLikeAnErrorKeyIsRejected() {
        assertThatThrownBy(() -> ProblemType.from("shipment.handoff_expired"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
