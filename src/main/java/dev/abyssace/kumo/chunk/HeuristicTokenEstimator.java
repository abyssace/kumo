package dev.abyssace.kumo.chunk;

import java.util.Objects;

/**
 * Patrón: Strategy concreta.
 *
 * <p>Heurística habitual para modelos tipo BPE: aproximadamente cuatro
 * caracteres por token. Se redondea hacia arriba para no quedarse corto, que es
 * el lado seguro cuando el límite lo impone el modelo de embeddings.</p>
 */
public final class HeuristicTokenEstimator implements TokenEstimator {

    /** Caracteres por token que asume la heurística. */
    public static final int CHARS_PER_TOKEN = 4;

    @Override
    public int estimate(String text) {
        Objects.requireNonNull(text, "text");
        if (text.isBlank()) {
            return 0;
        }
        return Math.max(1, (text.length() + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN);
    }
}
