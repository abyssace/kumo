package dev.abyssace.kumo.domain;

import java.util.List;
import java.util.Objects;

/**
 * Patrón: Value Object (record inmutable).
 *
 * <p>Respuesta completa del asistente: la pregunta original, el texto generado
 * y las fuentes utilizadas. Si no hubo fuentes, {@link #citations()} está
 * vacío.</p>
 */
public record Answer(String question, String text, List<Citation> citations) {

    public Answer {
        Objects.requireNonNull(question, "question");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(citations, "citations");
        citations = List.copyOf(citations);
    }

    public boolean hasCitations() {
        return !citations.isEmpty();
    }
}
