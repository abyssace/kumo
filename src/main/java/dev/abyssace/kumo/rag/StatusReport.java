package dev.abyssace.kumo.rag;

import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.store.IndexMetadata;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Patrón: Value Object (record inmutable) + View Model.
 *
 * <p>Fotografía del entorno para el comando {@code status}. Separa lo que se
 * pudo observar de Ollama y del índice de lo que es la configuración, de modo
 * que la consola no tenga que hacer comprobaciones ni capturar excepciones.</p>
 */
public record StatusReport(
        Path configFile,
        AppConfig config,
        Path corpus,
        OllamaStatus ollama,
        IndexStatus index) {

    public StatusReport {
        Objects.requireNonNull(configFile, "configFile");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(corpus, "corpus");
        Objects.requireNonNull(ollama, "ollama");
        Objects.requireNonNull(index, "index");
    }

    /** Estado observado de Ollama. */
    public record OllamaStatus(
            boolean reachable,
            Optional<String> error,
            List<String> installedModels,
            boolean chatModelInstalled,
            boolean embeddingModelInstalled,
            OptionalInt embeddingContextLength) {

        public OllamaStatus {
            Objects.requireNonNull(error, "error");
            Objects.requireNonNull(installedModels, "installedModels");
            Objects.requireNonNull(embeddingContextLength, "embeddingContextLength");
            installedModels = List.copyOf(installedModels);
        }

        /** Indica si el fragmento configurado cabe en el contexto observado. */
        public boolean chunkFitsContext(int chunkSizeTokens) {
            return embeddingContextLength.isEmpty()
                    || chunkSizeTokens < embeddingContextLength.getAsInt();
        }
    }

    /** Estado observado del índice del corpus. */
    public record IndexStatus(
            Path indexFile,
            Optional<IndexMetadata> metadata,
            Optional<String> error) {

        public IndexStatus {
            Objects.requireNonNull(indexFile, "indexFile");
            Objects.requireNonNull(metadata, "metadata");
            Objects.requireNonNull(error, "error");
        }

        public boolean exists() {
            return metadata.isPresent();
        }

        public boolean isCorrupt() {
            return error.isPresent();
        }
    }
}
