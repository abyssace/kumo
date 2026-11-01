package dev.abyssace.kumo.store;

/**
 * Patrón: Strategy.
 *
 * <p>Métrica de similitud entre vectores. El coseno es el valor por defecto,
 * pero el store no queda acoplado a él.</p>
 */
public interface SimilarityMetric {

    double score(float[] a, float[] b);

    /** Nombre legible para configuración y diagnóstico. */
    String name();
}
