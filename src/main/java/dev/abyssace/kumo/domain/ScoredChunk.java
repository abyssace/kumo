package dev.abyssace.kumo.domain;

import java.util.Objects;

/**
 * Patrón: Value Object (record inmutable) + Comparable.
 *
 * <p>Resultado de una búsqueda vectorial. El orden natural es de mayor a menor
 * puntaje y, en caso de empate, por identificador para que la salida sea
 * determinista.</p>
 *
 * @param embedded fragmento con su vector
 * @param score    similitud con la consulta (coseno por defecto)
 */
public record ScoredChunk(EmbeddedChunk embedded, double score) implements Comparable<ScoredChunk> {

    public ScoredChunk {
        Objects.requireNonNull(embedded, "embedded");
        if (Double.isNaN(score)) {
            throw new IllegalArgumentException("El puntaje no puede ser NaN");
        }
    }

    @Override
    public int compareTo(ScoredChunk other) {
        int byScore = Double.compare(other.score, score);
        return byScore != 0
                ? byScore
                : embedded.chunk().id().compareTo(other.embedded.chunk().id());
    }
}
