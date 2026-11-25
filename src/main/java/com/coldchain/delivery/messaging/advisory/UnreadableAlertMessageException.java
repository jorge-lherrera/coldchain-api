package com.coldchain.delivery.messaging.advisory;

import java.io.Serial;

public class UnreadableAlertMessageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnreadableAlertMessageException(String message) {
        super(message);
    }

    public UnreadableAlertMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
