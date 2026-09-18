package com.coldchain.shared.observability;

public final class LogMessage {

    public static final String UNEXPECTED_FAILURE = "Unhandled exception reached the response boundary";

    public static final String UNTRANSLATED_CONSTRAINT =
            "A database constraint reached the response boundary untranslated: constraint={}";

    public static final String DENIED_UNAUTHENTICATED =
            "Denied a call that carried no usable credentials: method={} path={} reason={}";

    public static final String DENIED_SCOPE =
            "Denied an authenticated call that lacked the scope: method={} path={} reason={}";

    private LogMessage() {
    }
}
