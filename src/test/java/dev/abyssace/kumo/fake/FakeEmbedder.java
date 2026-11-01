package dev.abyssace.kumo.fake;

import dev.abyssace.kumo.embed.Embedder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Patrón: Test Double (Fake) de {@link Embedder}.
 *
 * <p>Asigna a cada palabra un eje fijo la primera vez que la ve y cuenta
 * apariciones. Es determinista, no usa red y hace que dos textos que comparten
 * palabras se parezcan, sin depender de Ollama.</p>
 */
public final class FakeEmbedder implements Embedder {

    private final int dimensions;
    private final Map<String, Integer> vocabulary = new HashMap<>();
    private int nextSlot;

    public FakeEmbedder() {
        this(1024);
    }

    public FakeEmbedder(int dimensions) {
        if (dimensions < 8) {
            throw new IllegalArgumentException("dimensions debe ser >= 8");
        }
        this.dimensions = dimensions;
    }

    @Override
    public synchronized float[] embed(String text) {
        Objects.requireNonNull(text, "text");

        float[] vector = new float[dimensions];
        for (String token : tokenize(text)) {
            vector[slotFor(token)] += 1.0f;
        }
        normalize(vector);
        return vector;
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        Objects.requireNonNull(texts, "texts");
        return texts.stream().map(this::embed).toList();
    }

    private int slotFor(String token) {
        Integer existing = vocabulary.get(token);
        if (existing != null) {
            return existing;
        }
        int slot = nextSlot % dimensions;
        nextSlot++;
        vocabulary.put(token, slot);
        return slot;
    }

    private static List<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(token -> !token.isBlank())
                .toList();
    }

    private static void normalize(float[] vector) {
        double norm = 0.0;
        for (float value : vector) {
            norm += (double) value * value;
        }
        if (norm == 0.0) {
            return;
        }
        float inverse = (float) (1.0 / Math.sqrt(norm));
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= inverse;
        }
    }
}
