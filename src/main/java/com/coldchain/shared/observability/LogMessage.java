package com.coldchain.shared.observability;

public final class LogMessage {

    public static final String UNEXPECTED_FAILURE = "Unhandled exception reached the response boundary";

    public static final String UNTRANSLATED_CONSTRAINT =
            "A database constraint reached the response boundary untranslated: constraint={}";

    private LogMessage() {
    }
}
