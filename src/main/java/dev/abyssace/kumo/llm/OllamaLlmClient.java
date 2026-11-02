package dev.abyssace.kumo.llm;

import dev.abyssace.kumo.llm.LlmClient;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Patrón: Adapter + Decorator.
 *
 * <p>Implementa {@link LlmClient} sobre Ollama. Siempre usa {@code think:false}
 * (lo fija {@link HttpOllamaClient}) y limpia los bloques de razonamiento tanto
 * en la respuesta completa como en el stream.</p>
 */
public final class OllamaLlmClient implements LlmClient {

    private final OllamaClient client;
    private final String model;
    private final int numCtx;

    public OllamaLlmClient(OllamaClient client, String model, int numCtx) {
        this.client = Objects.requireNonNull(client, "client");
        this.model = requireText(model, "model");
        if (numCtx < 1) {
            throw new IllegalArgumentException("numCtx debe ser >= 1, pero fue " + numCtx);
        }
        this.numCtx = numCtx;
    }

    @Override
    public String complete(String prompt) {
        Objects.requireNonNull(prompt, "prompt");
        String raw = client.chat(model, List.of(OllamaMessage.user(prompt)), numCtx, null);
        return ReasoningStripper.strip(raw);
    }

    @Override
    public void stream(String prompt, Consumer<String> onToken) {
        Objects.requireNonNull(prompt, "prompt");
        Objects.requireNonNull(onToken, "onToken");

        ReasoningFilter filter = new ReasoningFilter();
        client.chatStreaming(model, List.of(OllamaMessage.user(prompt)), numCtx, token -> {
            String safe = filter.feed(token);
            if (!safe.isEmpty()) {
                onToken.accept(safe);
            }
        });
        String tail = filter.finish();
        if (!tail.isEmpty()) {
            onToken.accept(tail);
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " no puede estar vacío");
        }
        return trimmed;
    }
}
