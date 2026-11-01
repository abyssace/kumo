package dev.abyssace.kumo.loader;

/**
 * Patrón: excepción de dominio de la capa de carga.
 *
 * <p>Envuelve errores de E/S y de formato para que la CLI muestre un mensaje
 * útil en lugar de una traza de PDFBox o de java.nio.</p>
 */
public class DocumentLoadException extends RuntimeException {

    public DocumentLoadException(String message) {
        super(message);
    }

    public DocumentLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
