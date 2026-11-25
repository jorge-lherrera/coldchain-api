package com.coldchain.triage.delivery.messaging;

import java.io.Serial;

public class UnreadableFeedMessageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnreadableFeedMessageException(String message) {
        super(message);
    }

    public UnreadableFeedMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
