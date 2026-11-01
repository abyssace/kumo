package dev.abyssace.kumo.config;

/**
 * Patrón: Value Object (enumeración).
 *
 * <p>Implementaciones de {@code VectorStore} seleccionables por configuración.
 * {@link #MEMORY} es útil para pruebas y sesiones efímeras; {@link #JSON}
 * persiste el índice en {@code <corpus>/.kumo/index.json}.</p>
 */
public enum StoreType {
    MEMORY,
    JSON
}
