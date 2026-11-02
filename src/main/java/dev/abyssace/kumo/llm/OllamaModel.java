package dev.abyssace.kumo.llm;

import java.util.Objects;

/**
 * Patrón: Value Object (record inmutable).
 *
 * @param name nombre completo del modelo, p. ej. {@code qwen3:8b}
 * @param size tamaño en bytes, o 0 si Ollama no lo informa
 */
public record OllamaModel(String name, long size) {

    public OllamaModel {
        Objects.requireNonNull(name, "name");
        name = name.strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("El nombre del modelo no puede estar vacío");
        }
        if (size < 0) {
            throw new IllegalArgumentException("El tamaño del modelo no puede ser negativo: " + size);
        }
    }
}
