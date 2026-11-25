package com.coldchain.triage.internal.domain.model;

public record TriageProvenance(String model, String promptVersion, Integer inputTokens, Integer outputTokens,
        Integer latencyMs, boolean contextAvailable) {

    public static TriageProvenance ofRules(boolean contextAvailable) {
        return new TriageProvenance(null, null, null, null, null, contextAvailable);
    }
}
