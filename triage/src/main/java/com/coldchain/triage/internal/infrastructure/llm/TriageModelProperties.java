package com.coldchain.triage.internal.infrastructure.llm;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The model triage asks. The effort is set even where it matches the model's default, so that a
 * change of model does not silently change how much it thinks. There are no sampling parameters:
 * the current models reject them, and the effort is the control.
 */
@ConfigurationProperties(prefix = "coldchain.triage.model")
public record TriageModelProperties(
        String apiKey,
        @DefaultValue("claude-opus-5-5") String name,
        @DefaultValue("medium") String effort,
        @DefaultValue("8000") int maxTokens,
        @DefaultValue("60s") Duration timeout,
        @DefaultValue("1") int maxRetries) {
}
