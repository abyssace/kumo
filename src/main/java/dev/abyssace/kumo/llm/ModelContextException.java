package dev.abyssace.kumo.llm;

/**
 * Patrón: excepción de dominio.
 *
 * <p>Indica que el tamaño de fragmento configurado no cabe en el contexto real
 * del modelo de embeddings. Se lanza antes de indexar para no truncar texto en
 * silencio.</p>
 */
public class ModelContextException extends RuntimeException {

    public ModelContextException(String message) {
        super(message);
    }
}
