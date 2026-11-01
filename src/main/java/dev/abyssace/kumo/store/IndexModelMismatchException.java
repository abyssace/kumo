package dev.abyssace.kumo.store;

/**
 * Patrón: excepción de dominio especializada.
 *
 * <p>El índice existe, pero se generó con otro modelo de embeddings. Mezclar
 * vectores de modelos distintos produce similitudes sin sentido, así que kumo
 * prefiere detenerse y pedir una reindexación antes que devolver basura.</p>
 */
public final class IndexModelMismatchException extends IndexFormatException {

    public IndexModelMismatchException(String message) {
        super(message);
    }
}
