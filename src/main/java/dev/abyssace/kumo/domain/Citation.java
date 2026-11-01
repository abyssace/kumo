package dev.abyssace.kumo.domain;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Patrón: Value Object (record inmutable) + Factory Method.
 *
 * <p>Fuente citada en una respuesta. Guarda archivo, ubicación, extracto y
 * puntaje para que la CLI pueda mostrar de dónde salió cada afirmación.</p>
 *
 * @param source     archivo de origen
 * @param location   ubicación dentro del archivo
 * @param chunkIndex índice del fragmento en el documento
 * @param excerpt    extracto corto del texto citado
 * @param score      similitud del fragmento con la consulta
 */
public record Citation(Path source, SourceLocation location, int chunkIndex, String excerpt, double score) {

    public Citation {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(excerpt, "excerpt");
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("El índice del fragmento debe ser >= 0, pero fue " + chunkIndex);
        }
        if (Double.isNaN(score)) {
            throw new IllegalArgumentException("El puntaje no puede ser NaN");
        }
    }

    /**
     * Construye una cita a partir de un resultado, recortando el extracto a
     * {@code maxExcerptChars} caracteres.
     */
    public static Citation from(ScoredChunk scored, int maxExcerptChars) {
        Objects.requireNonNull(scored, "scored");
        if (maxExcerptChars < 1) {
            throw new IllegalArgumentException("El tamaño del extracto debe ser >= 1");
        }
        Chunk chunk = scored.embedded().chunk();
        return new Citation(
                chunk.source(),
                chunk.location(),
                chunk.index(),
                excerpt(chunk.text(), maxExcerptChars),
                scored.score());
    }

    private static String excerpt(String text, int maxChars) {
        String clean = text.strip().replaceAll("\\s+", " ");
        if (clean.length() <= maxChars) {
            return clean;
        }
        return clean.substring(0, maxChars).stripTrailing() + "…";
    }
}
