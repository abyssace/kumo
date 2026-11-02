package dev.abyssace.kumo.rag;

import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.config.ConfigManager;
import dev.abyssace.kumo.llm.ModelNames;
import dev.abyssace.kumo.llm.OllamaClient;
import dev.abyssace.kumo.llm.OllamaException;
import dev.abyssace.kumo.llm.OllamaModel;
import dev.abyssace.kumo.llm.OllamaModelInfo;
import dev.abyssace.kumo.llm.OllamaUnavailableException;
import dev.abyssace.kumo.store.IndexFormatException;
import dev.abyssace.kumo.store.IndexMetadata;
import dev.abyssace.kumo.store.JsonVectorStore;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Patrón: Application Service + Facade de diagnóstico.
 *
 * <p>Reúne configuración, Ollama e índice sin lanzar excepciones por un
 * servicio caído: {@code status} debe poder contar qué falla. Solo propaga los
 * errores de configuración, que impiden saber qué se está comprobando.</p>
 */
public final class StatusService {

    private final ConfigManager configManager;
    private final OllamaClient ollamaClient;

    public StatusService(ConfigManager configManager, OllamaClient ollamaClient) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.ollamaClient = Objects.requireNonNull(ollamaClient, "ollamaClient");
    }

    public StatusReport report(Path corpus) {
        Objects.requireNonNull(corpus, "corpus");
        Path normalizedCorpus = corpus.toAbsolutePath().normalize();
        AppConfig config = configManager.load();
        Path indexFile = JsonVectorStore.indexPath(normalizedCorpus);

        return new StatusReport(
                configManager.configFile(),
                config,
                normalizedCorpus,
                inspectOllama(config),
                inspectIndex(indexFile));
    }

    private StatusReport.IndexStatus inspectIndex(Path indexFile) {
        try {
            Optional<IndexMetadata> metadata = JsonVectorStore.inspect(indexFile);
            return new StatusReport.IndexStatus(indexFile, metadata, Optional.empty());
        } catch (IndexFormatException ex) {
            return new StatusReport.IndexStatus(indexFile, Optional.empty(), Optional.of(ex.getMessage()));
        }
    }

    private StatusReport.OllamaStatus inspectOllama(AppConfig config) {
        List<OllamaModel> models;
        try {
            models = ollamaClient.listModels();
        } catch (OllamaUnavailableException ex) {
            return unavailable(ex);
        } catch (OllamaException ex) {
            return unavailable(ex);
        }

        List<String> names = models.stream().map(OllamaModel::name).toList();
        boolean chatInstalled = ModelNames.isInstalled(config.chatModel(), names);
        boolean embeddingInstalled = ModelNames.isInstalled(config.embeddingModel(), names);

        OptionalInt context = OptionalInt.empty();
        if (embeddingInstalled) {
            try {
                OllamaModelInfo info = ollamaClient.showModel(config.embeddingModel());
                context = info.contextLength();
            } catch (OllamaException ignored) {
                // El modelo aparece en /api/tags pero /api/show no respondió;
                // se muestra el estado sin contexto en lugar de fallar.
            }
        }

        return new StatusReport.OllamaStatus(
                true, Optional.empty(), names, chatInstalled, embeddingInstalled, context);
    }

    private static StatusReport.OllamaStatus unavailable(OllamaException ex) {
        return new StatusReport.OllamaStatus(
                false,
                Optional.of(ex.getMessage()),
                List.of(),
                false,
                false,
                OptionalInt.empty());
    }
}
