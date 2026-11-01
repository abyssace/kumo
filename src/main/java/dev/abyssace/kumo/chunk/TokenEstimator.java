package dev.abyssace.kumo.chunk;

/**
 * Patrón: Strategy.
 *
 * <p>Estima cuántos tokens consume un texto. Se aísla tras una interfaz porque
 * el tamaño real depende del tokenizador de cada modelo: hoy usamos una
 * heurística barata y mañana podría sustituirse por el tokenizador exacto sin
 * tocar el chunker.</p>
 */
public interface TokenEstimator {

    /** Tokens estimados para el texto; 0 si está vacío o solo tiene espacios. */
    int estimate(String text);
}
