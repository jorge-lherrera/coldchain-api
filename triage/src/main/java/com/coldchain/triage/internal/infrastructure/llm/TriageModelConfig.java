package com.coldchain.triage.internal.infrastructure.llm;

import com.anthropic.models.messages.OutputConfig;
import com.coldchain.triage.internal.domain.service.TriageAdvisor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

/**
 * The chat model exists only when a key is configured. Without one, triage keeps working on the rules
 * alone, which is how CI and the walkthrough run. The answer is requested as structured output rather
 * than through a forced tool call, which the current models reject while they think.
 */
@Configuration
@EnableConfigurationProperties(TriageModelProperties.class)
public class TriageModelConfig {

    private static final Logger LOG = LoggerFactory.getLogger(TriageModelConfig.class);

    static final ClassPathResource TEMPLATE = new ClassPathResource("prompts/" + TriagePrompt.VERSION + ".st");

    static final ClassPathResource SCHEMA =
            new ClassPathResource("prompts/" + TriagePrompt.VERSION + ".schema.json");

    @Bean
    @ConditionalOnExpression("!'${coldchain.triage.model.api-key:}'.isBlank()")
    ChatModel triageChatModel(TriageModelProperties properties) {
        return AnthropicChatModel.builder().options(optionsOf(properties)).build();
    }

    @Bean
    TriageAdvisor triageAdvisor(ObjectProvider<ChatModel> models, TriageModelProperties properties,
            JsonMapper json, Clock clock) {
        ChatModel model = models.getIfAvailable();
        if (model == null) {
            LOG.info("No model key is configured: every excursion is triaged by the rules");
            return (alert, context) -> Optional.empty();
        }
        return new SpringAiTriageAdvisor(model, new TriagePrompt(TEMPLATE), json, properties.name(), clock);
    }

    static AnthropicChatOptions optionsOf(TriageModelProperties properties) {
        return AnthropicChatOptions.builder()
                .apiKey(properties.apiKey())
                .model(properties.name())
                .maxTokens(properties.maxTokens())
                .effort(OutputConfig.Effort.of(properties.effort()))
                .outputSchema(schema())
                .timeout(properties.timeout())
                .maxRetries(properties.maxRetries())
                .build();
    }

    static String schema() {
        try {
            return SCHEMA.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The output schema " + SCHEMA.getPath() + " is missing", unreadable);
        }
    }
}
