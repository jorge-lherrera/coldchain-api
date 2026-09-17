package com.coldchain.shared.error;

import java.net.URI;

public final class ProblemType {

    private static final String BASE = "urn:coldchain:problem:";

    private static final String MESSAGE_KEY_PREFIX = "error.";

    private ProblemType() {
    }

    public static URI from(String messageKey) {
        if (!messageKey.startsWith(MESSAGE_KEY_PREFIX)) {
            throw new IllegalArgumentException("A message key starts with \"error.\", received " + messageKey);
        }
        String identifier = messageKey.substring(MESSAGE_KEY_PREFIX.length())
                .replace('.', ':')
                .replace('_', '-');
        return URI.create(BASE + identifier);
    }
}
