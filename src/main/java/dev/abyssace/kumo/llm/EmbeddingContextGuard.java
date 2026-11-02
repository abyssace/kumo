package dev.abyssace.kumo.llm;

import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.i18n.Messages;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Patrón: Guard / Specification.
 *
 * <p>Comprueba con {@code /api/show} que el fragmento configurado quepa en el
 * contexto real del modelo de embeddings. No confunde el contexto del modelo
 * con {@code num_ctx} del chat: son límites distintos. Si Ollama no publica la
 * clave {@code *.context_length}, se confía en el valor de configuración.</p>
 */
public final class EmbeddingContextGuard {

    private EmbeddingContextGuard() {
    }

    /**
     * Valida y devuelve la información del modelo para que la CLI pueda
     * mostrarla.
     *
     * @param config     configuración validada
     * @param client     cliente de Ollama
     * @param configFile ruta que se muestra en el mensaje de error
     */
    public static OllamaModelInfo validate(AppConfig config, OllamaClient client, Path configFile) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(configFile, "configFile");

        OllamaModelInfo info = client.showModel(config.embeddingModel());
        info.contextLength().ifPresent(limit -> {
            if (config.chunkSizeTokens() >= limit) {
                throw new ModelContextException(Messages.OLLAMA_CONTEXT_TOO_SMALL.formatted(
                        config.embeddingModel(),
                        limit,
                        config.chunkSizeTokens(),
                        configFile));
            }
        });
        return info;
    }
}
