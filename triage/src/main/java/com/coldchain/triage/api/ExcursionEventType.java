package com.coldchain.triage.api;

import java.util.Optional;
import java.util.stream.Stream;

public enum ExcursionEventType {

    OPENED("ExcursionOpened"),
    CLOSED("ExcursionClosed");

    private final String wireName;

    ExcursionEventType(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static Optional<ExcursionEventType> ofWireName(String wireName) {
        return Stream.of(values()).filter(type -> type.wireName.equals(wireName)).findFirst();
    }
}
