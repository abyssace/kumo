package dev.abyssace.kumo.embed;

import java.util.List;

/**
 * Patrón: Strategy (contrato) + Adapter (implementación Ollama en F3).
 *
 * <p>Convierte texto en vectores. Los tests usan un doble determinista, sin
 * red ni Ollama.</p>
 */
public interface Embedder {

    float[] embed(String text);

    List<float[]> embedAll(List<String> texts);
}
