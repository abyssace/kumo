package dev.abyssace.kumo.config;

import dev.abyssace.kumo.store.CosineSimilarity;
import dev.abyssace.kumo.store.InMemoryVectorStore;
import dev.abyssace.kumo.store.SimilarityMetric;
import dev.abyssace.kumo.store.VectorStore;
import java.util.Objects;

/**
 * Patrón: Factory Method + inyección de dependencias.
 *
 * <p>Punto único donde la configuración se convierte en un {@link VectorStore}.
 * Cambiar {@code vectorStore} en {@link AppConfig} es la única línea necesaria
 * para cambiar de implementación.</p>
 */
public final class StoreFactory {

    private StoreFactory() {
    }

    public static VectorStore create(AppConfig config) {
        return create(config, new CosineSimilarity());
    }

    public static VectorStore create(AppConfig config, SimilarityMetric metric) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(metric, "metric");

        return switch (config.vectorStore()) {
            case MEMORY -> new InMemoryVectorStore(metric);
            case JSON -> throw new UnsupportedOperationException(
                    "El store JSON se habilita en la fase 2. Por ahora usa vectorStore=MEMORY.");
        };
    }
}
