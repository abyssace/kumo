package dev.abyssace.kumo.store;

/**
 * Patrón: excepción de dominio de la capa de persistencia.
 *
 * <p>Indica que el archivo de índice no se puede interpretar: JSON corrupto,
 * versión de formato desconocida o metadatos incoherentes.</p>
 */
public class IndexFormatException extends RuntimeException {

    public IndexFormatException(String message) {
        super(message);
    }

    public IndexFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
