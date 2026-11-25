package com.coldchain.triage.internal.infrastructure.llm;

import com.coldchain.triage.internal.domain.model.ExcursionAlert;
import com.coldchain.triage.internal.domain.model.ExcursionContext;
import com.coldchain.triage.internal.domain.model.Triage;
import com.coldchain.triage.internal.domain.model.TriageProvenance;
import com.coldchain.triage.internal.domain.service.TriageAdvisor;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.FinishReasonCategory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Asks the model through Spring AI's chat model. The structured output it requests is a guarantee of
 * shape, not of sense, so the answer is still read as untrusted: any answer that is not a completed
 * turn, does not parse, or parses outside the closed sets is discarded whole, and the rules answer.
 */
final class SpringAiTriageAdvisor implements TriageAdvisor {

    private static final Logger LOG = LoggerFactory.getLogger(SpringAiTriageAdvisor.class);

    private final ChatModel model;

    private final TriagePrompt prompt;

    private final JsonMapper json;

    private final String configuredModel;

    private final Clock clock;

    SpringAiTriageAdvisor(ChatModel model, TriagePrompt prompt, JsonMapper json, String configuredModel,
            Clock clock) {
        this.model = model;
        this.prompt = prompt;
        this.json = json;
        this.configuredModel = configuredModel;
        this.clock = clock;
    }

    @Override
    public Optional<Triage> adviseOn(ExcursionAlert alert, ExcursionContext context) {
        long started = System.nanoTime();
        ChatResponse response;
        try {
            response = model.call(prompt.of(alert, context));
        } catch (RuntimeException failure) {
            LOG.warn("The model did not answer for excursion {}, so the rules will: {}", alert.excursionId(),
                    failure.toString());
            return Optional.empty();
        }
        int latencyMs = Math.toIntExact(Duration.ofNanos(System.nanoTime() - started).toMillis());
        Generation generation = response == null ? null : response.getResult();
        if (generation == null
                || generation.getMetadata().getFinishReasonCategory() != FinishReasonCategory.COMPLETED) {
            LOG.warn("The model stopped with {} for excursion {}, so the rules will answer",
                    stopReasonOf(generation), alert.excursionId());
            return Optional.empty();
        }
        Optional<TriageAnswer> answer = read(generation.getOutput().getText(), alert);
        Optional<String> flaw = answer.flatMap(TriageAnswer::flaw);
        if (flaw.isPresent()) {
            LOG.warn("The model answered {} for excursion {}: discarded, and the rules will answer", flaw.get(),
                    alert.excursionId());
            return Optional.empty();
        }
        return answer.map(valid -> valid.toTriage(provenanceOf(response, latencyMs), clock.instant()));
    }

    private Optional<TriageAnswer> read(String text, ExcursionAlert alert) {
        if (text == null || text.isBlank()) {
            LOG.warn("The model answered nothing for excursion {}, so the rules will", alert.excursionId());
            return Optional.empty();
        }
        try {
            return Optional.of(json.readValue(text, TriageAnswer.class));
        } catch (JacksonException unreadable) {
            LOG.warn("The model answered something that is not a triage for excursion {}, so the rules will: {}",
                    alert.excursionId(), unreadable.getOriginalMessage());
            return Optional.empty();
        }
    }

    private TriageProvenance provenanceOf(ChatResponse response, int latencyMs) {
        String answeredBy = response.getMetadata().getModel();
        Usage usage = response.getMetadata().getUsage();
        return new TriageProvenance(answeredBy == null || answeredBy.isBlank() ? configuredModel : answeredBy,
                TriagePrompt.VERSION, usage.getPromptTokens(), usage.getCompletionTokens(), latencyMs, true);
    }

    private static String stopReasonOf(Generation generation) {
        return generation == null ? "no answer at all" : "stop reason " + generation.getMetadata().getFinishReason();
    }
}
