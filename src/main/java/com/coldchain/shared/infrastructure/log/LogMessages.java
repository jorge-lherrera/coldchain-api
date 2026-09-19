package com.coldchain.shared.infrastructure.log;

public final class LogMessages {

    public static final String UNEXPECTED_FAILURE = "Unhandled exception reached the response boundary";

    public static final String UNTRANSLATED_CONSTRAINT =
            "A database constraint reached the response boundary untranslated: constraint={}";

    public static final String DENIED_UNAUTHENTICATED =
            "Denied a call that carried no usable credentials: method={} path={} reason={}";

    public static final String DENIED_SCOPE =
            "Denied an authenticated call that lacked the scope: method={} path={} reason={}";

    public static final String I18N_BUNDLES_DISCOVERED =
            "Message catalogue assembled from the classpath: bundles={}";

    public static final String I18N_SCAN_FAILED =
            "A message bundle could not be read, so its language falls back to English: source={}";

    private LogMessages() {
    }
}
