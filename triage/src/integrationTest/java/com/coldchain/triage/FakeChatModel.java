package com.coldchain.triage;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.FinishReasonCategory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * The chat model of the integration tests. CI never calls a real one (ADR-012). Each answer it is
 * given is used once; asked with none left, it fails the way a model that does not answer fails.
 */
final class FakeChatModel implements ChatModel {

    static final String MODEL = "claude-opus-5-5";

    static final int INPUT_TOKENS = 1200;

    static final int OUTPUT_TOKENS = 300;

    private final Queue<String> answers = new ConcurrentLinkedQueue<>();

    private final AtomicInteger asked = new AtomicInteger();

    void willAnswer(String json) {
        answers.add(json);
    }

    int timesAsked() {
        return asked.get();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        asked.incrementAndGet();
        String answer = answers.poll();
        if (answer == null) {
            throw new IllegalStateException("The model is not answering");
        }
        Generation generation = new Generation(new AssistantMessage(answer), ChatGenerationMetadata.builder()
                .finishReason("end_turn")
                .finishReasonCategory(FinishReasonCategory.COMPLETED)
                .build());
        return new ChatResponse(List.of(generation), ChatResponseMetadata.builder()
                .model(MODEL)
                .usage(new DefaultUsage(INPUT_TOKENS, OUTPUT_TOKENS))
                .build());
    }
}
