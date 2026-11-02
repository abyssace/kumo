package dev.abyssace.kumo.store;

import java.util.Objects;

/**
 * Patrón: Value Object (record inmutable).
 *
 * <p>Metadatos del índice que se pueden leer sin abrir el store ni validar el
 * modelo configurado. {@code status} los usa para mostrar un índice antiguo o
 * incompatible en lugar de fallar con una traza.</p>
 *
 * @param embeddingModel modelo con el que se generaron los vectores
 * @param dimensions     dimensión de los vectores, o -1 si está vacío
 * @param chunks         número de fragmentos guardados
 */
public record IndexMetadata(String embeddingModel, int dimensions, int chunks) {

    public IndexMetadata {
        Objects.requireNonNull(embeddingModel, "embeddingModel");
        if (dimensions < -1) {
            throw new IllegalArgumentException("dimensions debe ser >= -1, pero fue " + dimensions);
        }
        if (chunks < 0) {
            throw new IllegalArgumentException("chunks debe ser >= 0, pero fue " + chunks);
        }
    }
}
