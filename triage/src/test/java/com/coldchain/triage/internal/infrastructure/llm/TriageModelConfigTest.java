package com.coldchain.triage.internal.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.anthropic.models.messages.OutputConfig;
import com.coldchain.triage.api.ProbableCause;
import com.coldchain.triage.api.Severity;
import com.coldchain.triage.internal.domain.service.TriageAdvisor;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class TriageModelConfigTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(TriageModelConfig.class)
            .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
            .withBean(Clock.class, Clock::systemUTC);

    @Test
    void withoutAKeyThereIsNoModelAndTheRulesAnswerEveryExcursion() {
        context.withPropertyValues("coldchain.triage.model.api-key=").run(started -> {
            assertThat(started).doesNotHaveBean(ChatModel.class);
            assertThat(started.getBean(TriageAdvisor.class).adviseOn(null, null)).isEmpty();
        });
    }

    @Test
    void withAKeyTheModelIsAsked() {
        context.withPropertyValues("coldchain.triage.model.api-key=sk-ant-test").run(started -> {
            assertThat(started).hasSingleBean(ChatModel.class);
            assertThat(started.getBean(TriageAdvisor.class)).isInstanceOf(SpringAiTriageAdvisor.class);
        });
    }

    @Test
    void theRequestNamesTheModelAndTheEffortAndAsksForStructuredOutput() {
        AnthropicChatOptions options = TriageModelConfig.optionsOf(new TriageModelProperties("sk-ant-test",
                "claude-opus-5-5", "medium", 8000, Duration.ofSeconds(60), 1));

        assertThat(options.getModel()).isEqualTo("claude-opus-5-5");
        assertThat(options.getMaxTokens()).isEqualTo(8000);
        assertThat(options.getTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(options.getOutputConfig().effort()).contains(OutputConfig.Effort.MEDIUM);
        assertThat(options.getOutputConfig().format())
                .describedAs("structured output, not a forced tool call, which the model rejects while it thinks")
                .isPresent();
    }

    @Test
    void theSchemaOffersExactlyTheCausesAndSeveritiesTheDomainKnows() {
        JsonNode properties = JsonMapper.builder().build().readTree(TriageModelConfig.schema()).get("properties");

        assertThat(valuesOf(properties.get("probableCause").get("enum")))
                .containsExactlyElementsOf(Arrays.stream(ProbableCause.values()).map(Enum::name).toList());
        assertThat(valuesOf(properties.get("severity").get("enum")))
                .containsExactlyElementsOf(Arrays.stream(Severity.values()).map(Enum::name).toList());
    }

    private static List<String> valuesOf(JsonNode array) {
        return array.valueStream().map(JsonNode::asString).toList();
    }
}
