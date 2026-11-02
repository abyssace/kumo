package dev.abyssace.kumo.llm;

/**
 * Patrón: excepción especializada.
 *
 * <p>Indica que no se pudo establecer conexión con Ollama. La CLI la distingue
 * de otros errores para recordar cómo arrancar el servidor local.</p>
 */
public class OllamaUnavailableException extends OllamaException {

    public OllamaUnavailableException(String message) {
        super(message);
    }

    public OllamaUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
