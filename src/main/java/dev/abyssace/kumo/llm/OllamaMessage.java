package dev.abyssace.kumo.llm;

import java.util.Objects;

/**
 * Patrón: Value Object (record inmutable) + Factory Method.
 *
 * <p>Mensaje del protocolo de chat de Ollama. Los roles son los que entiende la
 * API: {@code system}, {@code user} y {@code assistant}.</p>
 */
public record OllamaMessage(String role, String content) {

    public OllamaMessage {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(content, "content");
        role = role.strip();
        if (role.isEmpty()) {
            throw new IllegalArgumentException("El rol del mensaje no puede estar vacío");
        }
    }

    public static OllamaMessage system(String content) {
        return new OllamaMessage("system", content);
    }

    public static OllamaMessage user(String content) {
        return new OllamaMessage("user", content);
    }

    public static OllamaMessage assistant(String content) {
        return new OllamaMessage("assistant", content);
    }
}
