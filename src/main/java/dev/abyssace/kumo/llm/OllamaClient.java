package dev.abyssace.kumo.llm;

import java.util.List;
import java.util.function.Consumer;

/**
 * Patrón: Port / Adapter.
 *
 * <p>Contrato de bajo nivel con la API de Ollama. La implementación real
 * ({@link HttpOllamaClient}) usa {@code java.net.http.HttpClient}; los tests
 * pueden sustituirla por un doble sin abrir un puerto. No es una de las cinco
 * interfaces centrales del dominio: es el puerto técnico del adaptador.</p>
 */
public interface OllamaClient {

    /** Modelos instalados, tal como los devuelve {@code /api/tags}. */
    List<OllamaModel> listModels();

    /** Información de un modelo, incluido su contexto si Ollama lo publica. */
    OllamaModelInfo showModel(String model);

    /** Embebe un lote de textos con {@code /api/embed}. */
    List<float[]> embed(String model, List<String> inputs);

    /**
     * Chat sin streaming. {@code formatJson} es un esquema JSON opcional para
     * salida estructurada; {@code null} lo omite.
     */
    String chat(String model, List<OllamaMessage> messages, int numCtx, String formatJson);

    /** Chat en streaming; reenvía cada trozo de contenido a {@code onToken}. */
    void chatStreaming(String model, List<OllamaMessage> messages, int numCtx, Consumer<String> onToken);
}
