package dev.abyssace.kumo.llm;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Patrón: Value Object (record inmutable).
 *
 * @param name          nombre del modelo consultado
 * @param contextLength contexto publicado por {@code /api/show}, si existe
 * @param capabilities  capacidades declaradas (chat, embedding, ...)
 */
public record OllamaModelInfo(String name, OptionalInt contextLength, List<String> capabilities) {

    public OllamaModelInfo {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(contextLength, "contextLength");
        Objects.requireNonNull(capabilities, "capabilities");
        capabilities = List.copyOf(capabilities);
    }

    public boolean supportsEmbedding() {
        return capabilities.stream().anyMatch(capability -> capability.equalsIgnoreCase("embedding"));
    }
}
