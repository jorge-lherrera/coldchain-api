package com.coldchain.triage.internal.domain.model;

public record Summary(String english, String spanish, String portuguese) {

    public Summary {
        requireText(english);
        requireText(spanish);
        requireText(portuguese);
    }

    private static void requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("A summary is written in all three languages the API answers in");
        }
    }
}
