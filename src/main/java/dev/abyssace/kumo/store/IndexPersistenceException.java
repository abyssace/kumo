package dev.abyssace.kumo.store;

/**
 * Patrón: excepción de dominio de la capa de persistencia.
 *
 * <p>Fallo al escribir o borrar el índice en disco. Se separa de
 * {@link IndexFormatException} porque el archivo puede ser válido y aun así no
 * poder guardarse (permisos, disco lleno).</p>
 */
public final class IndexPersistenceException extends RuntimeException {

    public IndexPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
