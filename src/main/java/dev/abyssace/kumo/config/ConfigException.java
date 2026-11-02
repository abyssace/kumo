package dev.abyssace.kumo.config;

/**
 * Patrón: excepción de dominio de la capa de configuración.
 *
 * <p>Envuelve errores de lectura/escritura del archivo global para que la CLI
 * muestre un mensaje en español en lugar de una traza de Jackson o de
 * {@code java.nio}.</p>
 */
public class ConfigException extends RuntimeException {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
