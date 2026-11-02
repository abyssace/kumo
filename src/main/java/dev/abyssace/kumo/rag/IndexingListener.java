package dev.abyssace.kumo.rag;

import java.nio.file.Path;

/**
 * Patrón: Observer.
 *
 * <p>La capa de indexado avisa del progreso sin saber si el destino es la
 * consola, una barra de progreso o un test. Todos los métodos tienen una
 * implementación vacía para que los dobles de prueba solo sobrescriban lo que
 * necesitan.</p>
 */
public interface IndexingListener {

    /** Empieza el trabajo; {@code totalFiles} ya excluye carpetas ignoradas. */
    default void onStart(int totalFiles) {
    }

    /** Va a procesar {@code file}; {@code index} es 1-based. */
    default void onFileStarted(Path file, int index) {
    }

    /** El archivo se indexó con {@code chunks} fragmentos. */
    default void onFileIndexed(Path file, int chunks) {
    }

    /** El archivo falló, pero el resto del lote puede continuar. */
    default void onFileFailed(Path file, String message) {
    }

    /** Terminó el lote (aunque haya habido fallos por archivo). */
    default void onFinished(IndexingSummary summary) {
    }
}
