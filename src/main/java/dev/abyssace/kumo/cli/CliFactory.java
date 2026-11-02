package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.chunk.FixedSizeChunker;
import dev.abyssace.kumo.chunk.HeuristicTokenEstimator;
import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.config.StoreFactory;
import dev.abyssace.kumo.embed.Embedder;
import dev.abyssace.kumo.llm.HttpOllamaClient;
import dev.abyssace.kumo.llm.OllamaClient;
import dev.abyssace.kumo.llm.OllamaEmbedder;
import dev.abyssace.kumo.llm.OllamaLlmClient;
import dev.abyssace.kumo.loader.DocumentLoaderFactory;
import dev.abyssace.kumo.prompt.RagPromptBuilder;
import dev.abyssace.kumo.rag.ChunkIndexer;
import dev.abyssace.kumo.rag.IndexingService;
import dev.abyssace.kumo.rag.RagService;
import dev.abyssace.kumo.store.VectorStore;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Patrón: Factory + inyección de dependencias.
 *
 * <p>Único lugar donde la CLI convierte configuración en objetos reales. Los
 * comandos no construyen clientes HTTP ni stores: piden lo que necesitan y así
 * las pruebas pueden sustituir las piezas de red por dobles.</p>
 */
public final class CliFactory {

    private CliFactory() {
    }

    public static HttpOllamaClient ollama(AppConfig config) {
        Objects.requireNonNull(config, "config");
        return new HttpOllamaClient(config.ollamaBaseUrl());
    }

    public static Embedder embedder(AppConfig config, OllamaClient client) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(client, "client");
        return new OllamaEmbedder(client, config.embeddingModel());
    }

    public static OllamaLlmClient llm(AppConfig config, OllamaClient client) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(client, "client");
        return new OllamaLlmClient(client, config.chatModel(), config.numCtx());
    }

    public static VectorStore store(AppConfig config, Path corpus) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(corpus, "corpus");
        return StoreFactory.create(config, corpus);
    }

    public static FixedSizeChunker chunker(AppConfig config) {
        Objects.requireNonNull(config, "config");
        return new FixedSizeChunker(
                new HeuristicTokenEstimator(),
                config.chunkSizeTokens(),
                config.chunkOverlapTokens());
    }

    public static RagService rag(
            AppConfig config, VectorStore store, OllamaClient client, int topK) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(client, "client");
        return new RagService(
                embedder(config, client),
                store,
                llm(config, client),
                new RagPromptBuilder(),
                topK,
                config.minScore());
    }

    public static IndexingService indexing(AppConfig config, Path corpus, OllamaClient client) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(corpus, "corpus");
        Objects.requireNonNull(client, "client");
        return new IndexingService(
                new DocumentLoaderFactory(),
                chunker(config),
                new ChunkIndexer(embedder(config, client), store(config, corpus)));
    }
}
