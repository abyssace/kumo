package dev.abyssace.kumo.llm;

/**
 * Patrón: excepción de dominio de la capa Ollama.
 *
 * <p>Agrupa errores HTTP, respuestas inesperadas y fallos de protocolo para que
 * la CLI muestre un mensaje útil en español.</p>
 */
public class OllamaException extends RuntimeException {

    public OllamaException(String message) {
        super(message);
    }

    public OllamaException(String message, Throwable cause) {
        super(message, cause);
    }
}
