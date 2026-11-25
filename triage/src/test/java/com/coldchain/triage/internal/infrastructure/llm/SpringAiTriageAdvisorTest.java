package com.coldchain.triage.internal.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.coldchain.triage.api.ExcursionKind;
import com.coldchain.triage.api.ProbableCause;
import com.coldchain.triage.api.TriageSource;
import com.coldchain.triage.internal.domain.model.AlertStatus;
import com.coldchain.triage.internal.domain.model.ContextHandoff;
import com.coldchain.triage.internal.domain.model.ContextReading;
import com.coldchain.triage.internal.domain.model.CustodyParticipation;
import com.coldchain.triage.internal.domain.model.ExcursionAlert;
import com.coldchain.triage.internal.domain.model.ExcursionContext;
import com.coldchain.triage.internal.domain.model.FrozenThresholds;
import com.coldchain.triage.internal.domain.model.Triage;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.FinishReasonCategory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import tools.jackson.databind.json.JsonMapper;

class SpringAiTriageAdvisorTest {

    private static final Instant NOW = Instant.parse("2026-05-01T11:00:00Z");

    private static final UUID SHIPMENT = UUID.fromString("01930000-0000-7000-8000-00000000000a");

    private static final UUID ORGANIZATION = UUID.fromString("01930000-0000-7000-8000-00000000000b");

    private static final UUID EXCURSION = UUID.fromString("01930000-0000-7000-8000-00000000000c");

    private static final String DOOR_OPENING = """
            {"probableCause":"DOOR_OPENING","severity":"LOW","confidence":0.8,\
            "summary":{"en":"A short spike that recovered on its own.","es":"Un pico corto que se recuperó solo.",\
            "pt":"Um pico curto que se recuperou sozinho."},\
            "recommendedAction":"Keep the doors shut while loading."}""";

    private static final ExcursionAlert CLOSED = ExcursionAlert.restore(UUID.randomUUID(), EXCURSION, SHIPMENT,
            ORGANIZATION, ExcursionKind.ABOVE_MAX, AlertStatus.CLOSED, Instant.parse("2026-05-01T10:05:00Z"),
            Instant.parse("2026-05-01T10:20:00Z"), new BigDecimal("11.50"), 15L, null, 0);

    private static final ExcursionContext CONTEXT = new ExcursionContext(Instant.parse("2026-05-01T09:35:00Z"),
            Instant.parse("2026-05-01T10:50:00Z"),
            List.of(new ContextReading(Instant.parse("2026-05-01T09:35:00Z"), new BigDecimal("4.50")),
                    new ContextReading(Instant.parse("2026-05-01T10:05:00Z"), new BigDecimal("11.50")),
                    new ContextReading(Instant.parse("2026-05-01T10:20:00Z"), new BigDecimal("5.00"))),
            new FrozenThresholds(new BigDecimal("2.00"), new BigDecimal("8.00"), 60, 120),
            CustodyParticipation.CARRIER, 25L,
            List.of(new ContextHandoff(Instant.parse("2026-05-01T09:40:00Z"), CustodyParticipation.SHIPPER,
                    CustodyParticipation.CARRIER)));

    private final List<Prompt> asked = new ArrayList<>();

    @Test
    void aCompletedAnswerBecomesATriageThatSaysWhoAnsweredAndAtWhatCost() {
        Optional<Triage> triage = advisorAnswering(answer(DOOR_OPENING, "end_turn", FinishReasonCategory.COMPLETED))
                .adviseOn(CLOSED, CONTEXT);

        assertThat(triage).hasValueSatisfying(advised -> {
            assertThat(advised.source()).isEqualTo(TriageSource.LLM);
            assertThat(advised.probableCause()).isEqualTo(ProbableCause.DOOR_OPENING);
            assertThat(advised.triagedAt()).isEqualTo(NOW);
            assertThat(advised.provenance().model()).isEqualTo("claude-opus-5-5-20261101");
            assertThat(advised.provenance().promptVersion()).isEqualTo("triage-v1");
            assertThat(advised.provenance().inputTokens()).isEqualTo(1200);
            assertThat(advised.provenance().outputTokens()).isEqualTo(300);
            assertThat(advised.provenance().latencyMs()).isNotNull().isNotNegative();
            assertThat(advised.provenance().contextAvailable()).isTrue();
        });
    }

    @Test
    void thePromptCarriesMeasurementsAndCustodyAndNothingThatIdentifiesAnyone() {
        advisorAnswering(answer(DOOR_OPENING, "end_turn", FinishReasonCategory.COMPLETED)).adviseOn(CLOSED, CONTEXT);

        String prompt = asked.getFirst().getContents();
        assertThat(prompt)
                .contains("ABOVE_MAX", "Peak reading: 11.5 °C", "Range: 2 to 8 °C", "allowed 60 minutes")
                .contains("by the CARRIER", "2026-05-01T09:40:00Z from the SHIPPER to the CARRIER")
                .contains("2026-05-01T10:05:00Z 11.5");
        assertThat(prompt)
                .describedAs("the model sees what a sensor and a custody log show, and no identifier (ADR-012)")
                .doesNotContain(SHIPMENT.toString(), ORGANIZATION.toString(), EXCURSION.toString());
    }

    @Test
    void anAnswerWithNoModelNamedIsCreditedToTheConfiguredOne() {
        ChatResponse unnamed = new ChatResponse(List.of(generation(DOOR_OPENING, "end_turn",
                FinishReasonCategory.COMPLETED)), ChatResponseMetadata.builder().usage(new DefaultUsage(10, 5)).build());

        Optional<Triage> triage = advisorAnswering(unnamed).adviseOn(CLOSED, CONTEXT);

        assertThat(triage).hasValueSatisfying(advised ->
                assertThat(advised.provenance().model()).isEqualTo("claude-opus-5-5"));
    }

    @Test
    void aRefusalIsLeftToTheRules() {
        Optional<Triage> triage = advisorAnswering(answer("", "refusal", FinishReasonCategory.FILTERED))
                .adviseOn(CLOSED, CONTEXT);

        assertThat(triage).isEmpty();
    }

    @Test
    void anAnswerCutShortIsLeftToTheRules() {
        Optional<Triage> triage = advisorAnswering(
                answer("{\"probableCause\":\"DOOR_OPENING\",\"sever", "max_tokens", FinishReasonCategory.TRUNCATED))
                .adviseOn(CLOSED, CONTEXT);

        assertThat(triage).isEmpty();
    }

    @Test
    void aModelThatDoesNotAnswerIsLeftToTheRules() {
        ChatModel down = prompt -> {
            throw new IllegalStateException("Request timed out");
        };

        Optional<Triage> triage = new SpringAiTriageAdvisor(down, prompt(), JsonMapper.builder().build(),
                "claude-opus-5-5", Clock.fixed(NOW, ZoneOffset.UTC)).adviseOn(CLOSED, CONTEXT);

        assertThat(triage).isEmpty();
    }

    @Test
    void proseInsteadOfATriageIsLeftToTheRules() {
        Optional<Triage> triage = advisorAnswering(
                answer("It was probably the door.", "end_turn", FinishReasonCategory.COMPLETED))
                .adviseOn(CLOSED, CONTEXT);

        assertThat(triage).isEmpty();
    }

    @Test
    void anAnswerOutsideTheSchemaIsDiscardedWhole() {
        Optional<Triage> triage = advisorAnswering(answer(DOOR_OPENING.replace("0.8", "1.2"), "end_turn",
                FinishReasonCategory.COMPLETED)).adviseOn(CLOSED, CONTEXT);

        assertThat(triage).isEmpty();
    }

    private SpringAiTriageAdvisor advisorAnswering(ChatResponse response) {
        ChatModel model = prompt -> {
            asked.add(prompt);
            return response;
        };
        return new SpringAiTriageAdvisor(model, prompt(), JsonMapper.builder().build(), "claude-opus-5-5",
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static TriagePrompt prompt() {
        return new TriagePrompt(TriageModelConfig.TEMPLATE);
    }

    private static ChatResponse answer(String text, String stopReason, FinishReasonCategory category) {
        return new ChatResponse(List.of(generation(text, stopReason, category)), ChatResponseMetadata.builder()
                .model("claude-opus-5-5-20261101")
                .usage(new DefaultUsage(1200, 300))
                .build());
    }

    private static Generation generation(String text, String stopReason, FinishReasonCategory category) {
        return new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(stopReason).finishReasonCategory(category).build());
    }
}
