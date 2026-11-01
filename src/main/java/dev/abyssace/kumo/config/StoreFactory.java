package dev.abyssace.kumo.config;

import dev.abyssace.kumo.store.CosineSimilarity;
import dev.abyssace.kumo.store.InMemoryVectorStore;
import dev.abyssace.kumo.store.JsonVectorStore;
import dev.abyssace.kumo.store.SimilarityMetric;
import dev.abyssace.kumo.store.VectorStore;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Patrón: Factory Method + inyección de dependencias.
 *
 * <p>Punto único donde la configuración se convierte en un {@link VectorStore}.
 * Cambiar {@code vectorStore} en {@link AppConfig} es la única línea necesaria
 * para cambiar de implementación. El store JSON necesita además la carpeta del
 * corpus, porque el índice vive junto a los documentos que describe.</p>
 */
public final class StoreFactory {

    private StoreFactory() {
    }

    /** Crea el store configurado. Solo válido para {@code MEMORY}. */
    public static VectorStore create(AppConfig config) {
        return create(config, new CosineSimilarity());
    }

    /** Crea el store configurado con una métrica explícita. Solo válido para {@code MEMORY}. */
    public static VectorStore create(AppConfig config, SimilarityMetric metric) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(metric, "metric");

        return switch (config.vectorStore()) {
            case MEMORY -> new InMemoryVectorStore(metric);
            case JSON -> throw new IllegalArgumentException(
                    "El store JSON necesita la carpeta del corpus: usa StoreFactory.create(config, corpusDir).");
        };
    }

    /** Crea el store configurado para un corpus concreto. */
    public static VectorStore create(AppConfig config, Path corpusDir) {
        return create(config, corpusDir, new CosineSimilarity());
    }

    /** Crea el store configurado para un corpus concreto y una métrica explícita. */
    public static VectorStore create(AppConfig config, Path corpusDir, SimilarityMetric metric) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(metric, "metric");

        return switch (config.vectorStore()) {
            case MEMORY -> new InMemoryVectorStore(metric);
            case JSON -> new JsonVectorStore(
                    JsonVectorStore.indexPath(Objects.requireNonNull(corpusDir, "corpusDir")),
                    metric,
                    config.embeddingModel());
        };
    }
}
