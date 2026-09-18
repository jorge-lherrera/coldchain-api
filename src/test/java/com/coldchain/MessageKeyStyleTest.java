package com.coldchain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class MessageKeyStyleTest {

    private static final Pattern MESSAGE_KEY = Pattern.compile("\"([a-z][a-z0-9_]*(?:[.][a-z0-9_]+)+)\"");

    private static final Pattern THREE_SEGMENT_CANON =
            Pattern.compile("(error|success)[.][a-z]+[.][a-z][a-z0-9_]*");

    private static final List<String> AREAS =
            List.of("core", "identity", "catalog", "shipment", "telemetry", "compliance");

    @Test
    void everyMessageKeyFollowsTheThreeSegmentCanon() {
        List<String> keys = SourceTree.javaFiles(SourceTree.MAIN).stream()
                .filter(file -> file.getFileName().toString().endsWith("ErrorCode.java")
                        || file.getFileName().toString().endsWith("SuccessCode.java"))
                .map(SourceTree::read)
                .flatMap(text -> MESSAGE_KEY.matcher(text).results().map(result -> result.group(1)))
                .distinct()
                .sorted()
                .toList();

        assertThat(keys).isNotEmpty();
        assertThat(keys).allSatisfy(key -> {
            assertThat(THREE_SEGMENT_CANON.matcher(key).matches())
                    .describedAs("%s is not error.<area>.<reason> or success.<area>.<what> in "
                            + "snake_case, and the key is the part of the message that is a contract",
                            key)
                    .isTrue();
            assertThat(AREAS)
                    .describedAs("%s names an area the product does not have", key)
                    .contains(key.split("[.]")[1]);
        });
    }
}
