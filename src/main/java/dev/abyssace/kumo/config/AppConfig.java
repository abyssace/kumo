package dev.abyssace.kumo.config;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

/**
 * Patrón: Builder + Value Object inmutable.
 *
 * <p>Única fuente de verdad de la configuración. Los modelos nunca se escriben
 * en el resto del código: se leen de aquí. Todos los valores tienen un default
 * razonable y pueden sobrescribirse desde el archivo de configuración o la CLI
 * en fases posteriores.</p>
 */
public record AppConfig(
        String ollamaBaseUrl,
        String chatModel,
        String embeddingModel,
        int numCtx,
        int embeddingContextTokens,
        int chunkSizeTokens,
        int chunkOverlapTokens,
        int topK,
        double minScore,
        boolean verbose,
        StoreType vectorStore) {

    public static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";
    public static final String DEFAULT_CHAT_MODEL = "qwen3:8b";
    public static final String DEFAULT_EMBEDDING_MODEL = "embeddinggemma";
    public static final int DEFAULT_NUM_CTX = 8192;
    /** Límite de contexto del modelo de embeddings; se verifica con /api/show en F3. */
    public static final int DEFAULT_EMBEDDING_CONTEXT_TOKENS = 2048;
    /** Margen amplio: 400 de 2048 tokens (~20 %). */
    public static final int DEFAULT_CHUNK_SIZE_TOKENS = 400;
    public static final int DEFAULT_CHUNK_OVERLAP_TOKENS = 80;
    public static final int DEFAULT_TOP_K = 5;
    /** -1 desactiva el umbral: el coseno vive en [-1, 1]. */
    public static final double DEFAULT_MIN_SCORE = -1.0;

    public AppConfig {
        ollamaBaseUrl = requireText(ollamaBaseUrl, "ollamaBaseUrl");
        while (ollamaBaseUrl.endsWith("/")) {
            ollamaBaseUrl = ollamaBaseUrl.substring(0, ollamaBaseUrl.length() - 1);
        }
        chatModel = requireText(chatModel, "chatModel");
        embeddingModel = requireText(embeddingModel, "embeddingModel");
        Objects.requireNonNull(vectorStore, "vectorStore");

        if (numCtx < 1) {
            throw new IllegalArgumentException("numCtx debe ser >= 1, pero fue " + numCtx);
        }
        if (embeddingContextTokens < 1) {
            throw new IllegalArgumentException(
                    "embeddingContextTokens debe ser >= 1, pero fue " + embeddingContextTokens);
        }
        if (chunkSizeTokens < 1) {
            throw new IllegalArgumentException("chunkSizeTokens debe ser >= 1, pero fue " + chunkSizeTokens);
        }
        if (chunkOverlapTokens < 0) {
            throw new IllegalArgumentException(
                    "chunkOverlapTokens debe ser >= 0, pero fue " + chunkOverlapTokens);
        }
        if (chunkOverlapTokens >= chunkSizeTokens) {
            throw new IllegalArgumentException(
                    "El solapamiento (" + chunkOverlapTokens + ") debe ser menor que el tamaño del fragmento ("
                            + chunkSizeTokens + ")");
        }
        if (chunkSizeTokens >= embeddingContextTokens) {
            throw new IllegalArgumentException(
                    "El fragmento (" + chunkSizeTokens + " tokens) debe caber con margen en el contexto del "
                            + "modelo de embeddings (" + embeddingContextTokens + " tokens)");
        }
        if (topK < 1) {
            throw new IllegalArgumentException("topK debe ser >= 1, pero fue " + topK);
        }
        if (minScore < -1.0 || minScore > 1.0) {
            throw new IllegalArgumentException("minScore debe estar entre -1 y 1, pero fue " + minScore);
        }
        if (!isLoopback(ollamaBaseUrl)) {
            throw new IllegalArgumentException(
                    "kumo solo habla con Ollama en localhost; revisa ollamaBaseUrl: " + ollamaBaseUrl);
        }
    }

    public static AppConfig defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " no puede estar vacío");
        }
        return trimmed;
    }

    private static boolean isLoopback(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return false;
            }
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            String normalized = host.toLowerCase(Locale.ROOT);
            return normalized.equals("localhost")
                    || normalized.equals("127.0.0.1")
                    || normalized.equals("::1")
                    || normalized.equals("[::1]");
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /** Builder mutable que se convierte en un {@link AppConfig} inmutable. */
    public static final class Builder {

        private String ollamaBaseUrl = DEFAULT_OLLAMA_BASE_URL;
        private String chatModel = DEFAULT_CHAT_MODEL;
        private String embeddingModel = DEFAULT_EMBEDDING_MODEL;
        private int numCtx = DEFAULT_NUM_CTX;
        private int embeddingContextTokens = DEFAULT_EMBEDDING_CONTEXT_TOKENS;
        private int chunkSizeTokens = DEFAULT_CHUNK_SIZE_TOKENS;
        private int chunkOverlapTokens = DEFAULT_CHUNK_OVERLAP_TOKENS;
        private int topK = DEFAULT_TOP_K;
        private double minScore = DEFAULT_MIN_SCORE;
        private boolean verbose;
        private StoreType vectorStore = StoreType.MEMORY;

        private Builder() {
        }

        public Builder ollamaBaseUrl(String value) {
            this.ollamaBaseUrl = value;
            return this;
        }

        public Builder chatModel(String value) {
            this.chatModel = value;
            return this;
        }

        public Builder embeddingModel(String value) {
            this.embeddingModel = value;
            return this;
        }

        public Builder numCtx(int value) {
            this.numCtx = value;
            return this;
        }

        public Builder embeddingContextTokens(int value) {
            this.embeddingContextTokens = value;
            return this;
        }

        public Builder chunkSizeTokens(int value) {
            this.chunkSizeTokens = value;
            return this;
        }

        public Builder chunkOverlapTokens(int value) {
            this.chunkOverlapTokens = value;
            return this;
        }

        public Builder topK(int value) {
            this.topK = value;
            return this;
        }

        public Builder minScore(double value) {
            this.minScore = value;
            return this;
        }

        public Builder verbose(boolean value) {
            this.verbose = value;
            return this;
        }

        public Builder vectorStore(StoreType value) {
            this.vectorStore = value;
            return this;
        }

        public AppConfig build() {
            return new AppConfig(
                    ollamaBaseUrl,
                    chatModel,
                    embeddingModel,
                    numCtx,
                    embeddingContextTokens,
                    chunkSizeTokens,
                    chunkOverlapTokens,
                    topK,
                    minScore,
                    verbose,
                    vectorStore);
        }
    }
}
