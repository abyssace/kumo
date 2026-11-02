package dev.abyssace.kumo.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

/**
 * Patrón: Data Transfer Object (record) + Mapper.
 *
 * <p>Representación legible del archivo {@code ~/.kumo/config.json}. Los campos
 * son opcionales a propósito: un archivo parcial hereda los defaults y así el
 * usuario solo escribe lo que quiere cambiar. La conversión a {@link AppConfig}
 * valida el resultado con las mismas reglas que el resto del sistema.</p>
 *
 * <p>El default del archivo usa {@link StoreType#JSON} porque la CLI persiste
 * el índice de cada corpus. {@link AppConfig#defaults()} sigue usando MEMORY
 * para que la librería y los tests no toquen disco.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConfigFile(
        String ollamaBaseUrl,
        String chatModel,
        String embeddingModel,
        Integer numCtx,
        Integer embeddingContextTokens,
        Integer chunkSizeTokens,
        Integer chunkOverlapTokens,
        Integer topK,
        Double minScore,
        Boolean verbose,
        StoreType vectorStore,
        Double summaryInputRatio) {

    /** Configuración completa que se escribe la primera vez. */
    public static ConfigFile defaults() {
        return new ConfigFile(
                AppConfig.DEFAULT_OLLAMA_BASE_URL,
                AppConfig.DEFAULT_CHAT_MODEL,
                AppConfig.DEFAULT_EMBEDDING_MODEL,
                AppConfig.DEFAULT_NUM_CTX,
                AppConfig.DEFAULT_EMBEDDING_CONTEXT_TOKENS,
                AppConfig.DEFAULT_CHUNK_SIZE_TOKENS,
                AppConfig.DEFAULT_CHUNK_OVERLAP_TOKENS,
                AppConfig.DEFAULT_TOP_K,
                AppConfig.DEFAULT_MIN_SCORE,
                false,
                StoreType.JSON,
                AppConfig.DEFAULT_SUMMARY_INPUT_RATIO);
    }

    /** Vuelca una configuración validada al formato del archivo. */
    public static ConfigFile from(AppConfig config) {
        Objects.requireNonNull(config, "config");
        return new ConfigFile(
                config.ollamaBaseUrl(),
                config.chatModel(),
                config.embeddingModel(),
                config.numCtx(),
                config.embeddingContextTokens(),
                config.chunkSizeTokens(),
                config.chunkOverlapTokens(),
                config.topK(),
                config.minScore(),
                config.verbose(),
                config.vectorStore(),
                config.summaryInputRatio());
    }

    /**
     * Combina los campos presentes con los defaults. Los campos ausentes no se
     * inventan: los completa el Builder de {@link AppConfig}.
     */
    public AppConfig toAppConfig() {
        AppConfig.Builder builder = AppConfig.builder();
        if (summaryInputRatio != null) {
            builder.summaryInputRatio(summaryInputRatio);
        }
        if (ollamaBaseUrl != null) {
            builder.ollamaBaseUrl(ollamaBaseUrl);
        }
        if (chatModel != null) {
            builder.chatModel(chatModel);
        }
        if (embeddingModel != null) {
            builder.embeddingModel(embeddingModel);
        }
        if (numCtx != null) {
            builder.numCtx(numCtx);
        }
        if (embeddingContextTokens != null) {
            builder.embeddingContextTokens(embeddingContextTokens);
        }
        if (chunkSizeTokens != null) {
            builder.chunkSizeTokens(chunkSizeTokens);
        }
        if (chunkOverlapTokens != null) {
            builder.chunkOverlapTokens(chunkOverlapTokens);
        }
        if (topK != null) {
            builder.topK(topK);
        }
        if (minScore != null) {
            builder.minScore(minScore);
        }
        if (verbose != null) {
            builder.verbose(verbose);
        }
        builder.vectorStore(vectorStore == null ? StoreType.JSON : vectorStore);
        return builder.build();
    }
}
