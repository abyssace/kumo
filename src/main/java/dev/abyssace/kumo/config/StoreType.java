package dev.abyssace.kumo.config;

/**
 * Patrón: Value Object (enumeración).
 *
 * <p>Implementaciones de {@code VectorStore} seleccionables por configuración.
 * {@link #JSON} se habilitará en F2; existe ya para fijar el punto de cambio.</p>
 */
public enum StoreType {
    MEMORY,
    JSON
}
