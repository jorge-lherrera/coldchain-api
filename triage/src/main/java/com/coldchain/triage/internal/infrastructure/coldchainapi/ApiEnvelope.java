package com.coldchain.triage.internal.infrastructure.coldchainapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiEnvelope<T>(T data) {
}
