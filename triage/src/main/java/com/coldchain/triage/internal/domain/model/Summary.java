package com.coldchain.triage.internal.domain.model;

public record Summary(String english, String spanish, String portuguese) {

    public static final int MAX_LENGTH = 1000;

    public Summary {
        requireText(english);
        requireText(spanish);
        requireText(portuguese);
    }

    private static void requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("A summary is written in all three languages the API answers in");
        }
        if (text.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "A summary is a few sentences, at most " + MAX_LENGTH + " characters, received " + text.length());
        }
    }
}
