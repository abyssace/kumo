package dev.abyssace.kumo.store;

import java.util.Objects;

/**
 * Patrón: Strategy concreta.
 *
 * <p>Similitud coseno: 1 para vectores idénticos en dirección, 0 para
 * ortogonales y -1 para opuestos. Un vector nulo da 0 para evitar división por
 * cero.</p>
 */
public final class CosineSimilarity implements SimilarityMetric {

    @Override
    public double score(float[] a, float[] b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "Los vectores tienen dimensiones distintas: " + a.length + " y " + b.length);
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Override
    public String name() {
        return "coseno";
    }
}
