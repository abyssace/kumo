package dev.abyssace.kumo.llm;

import java.util.function.Consumer;

/**
 * Patrón: Port / Adapter.
 *
 * <p>Contrato mínimo de generación de texto. La implementación real hablará con
 * Ollama en F3; los tests usan un eco determinista.</p>
 */
public interface LlmClient {

    String complete(String prompt);

    void stream(String prompt, Consumer<String> onToken);
}
