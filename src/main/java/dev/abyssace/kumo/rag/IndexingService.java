package dev.abyssace.kumo.rag;

import dev.abyssace.kumo.chunk.Chunker;
import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.llm.OllamaException;
import dev.abyssace.kumo.llm.OllamaUnavailableException;
import dev.abyssace.kumo.loader.DocumentLoadException;
import dev.abyssace.kumo.loader.DocumentLoaderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Patrón: Application Service + Observer.
 *
 * <p>Orquesta el indexado de archivos y carpetas: descubre archivos, los carga,
 * los trocea, los embebe y reemplaza en el store los fragmentos anteriores del
 * mismo archivo. No conoce Ollama ni la consola; solo depende de las interfaces
 * del dominio y de {@link IndexingListener}.</p>
 */
public final class IndexingService {

    private static final Set<String> IGNORED_DIRECTORIES =
            Set.of(".kumo", ".git", "target", "node_modules", "build", "dist");

    private final DocumentLoaderFactory loaderFactory;
    private final Chunker chunker;
    private final ChunkIndexer chunkIndexer;

    public IndexingService(
            DocumentLoaderFactory loaderFactory,
            Chunker chunker,
            ChunkIndexer chunkIndexer) {
        this.loaderFactory = Objects.requireNonNull(loaderFactory, "loaderFactory");
        this.chunker = Objects.requireNonNull(chunker, "chunker");
        this.chunkIndexer = Objects.requireNonNull(chunkIndexer, "chunkIndexer");
    }

    /**
     * Indexa los archivos y carpetas indicados. Los errores de un archivo no
     * abortan el lote; los de Ollama o de formato del índice sí, porque afectan
     * a todos.
     */
    public IndexingSummary index(List<Path> inputs, IndexingListener listener) {
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(listener, "listener");

        List<Path> files = collectFiles(inputs);
        listener.onStart(files.size());

        int indexedFiles = 0;
        int indexedChunks = 0;
        int failures = 0;

        for (int i = 0; i < files.size(); i++) {
            Path file = files.get(i);
            listener.onFileStarted(file, i + 1);
            try {
                Document document = loaderFactory.load(file);
                List<Chunk> chunks = chunker.split(document);
                if (chunks.isEmpty()) {
                    failures++;
                    listener.onFileFailed(file, Messages.EMPTY_DOCUMENT);
                    continue;
                }
                int count = chunkIndexer.replace(file, chunks);
                indexedFiles++;
                indexedChunks += count;
                listener.onFileIndexed(file, count);
            } catch (OllamaUnavailableException ex) {
                throw ex;
            } catch (DocumentLoadException | OllamaException ex) {
                failures++;
                listener.onFileFailed(file, ex.getMessage());
            }
        }

        IndexingSummary summary = new IndexingSummary(indexedFiles, indexedChunks, failures);
        listener.onFinished(summary);
        return summary;
    }

    /** Extensiones que entiende la fábrica de loaders, para los mensajes. */
    public Set<String> supportedExtensions() {
        return loaderFactory.supportedExtensions();
    }

    private List<Path> collectFiles(List<Path> inputs) {
        LinkedHashSet<Path> files = new LinkedHashSet<>();
        for (Path input : inputs) {
            Objects.requireNonNull(input, "input");
            Path normalized = input.toAbsolutePath().normalize();
            if (!Files.exists(normalized)) {
                throw new DocumentLoadException(Messages.FILE_NOT_FOUND.formatted(normalized));
            }
            if (Files.isDirectory(normalized)) {
                collectDirectory(normalized, files);
            } else if (Files.isRegularFile(normalized)) {
                files.add(normalized);
            } else {
                throw new DocumentLoadException(Messages.FILE_NOT_FOUND.formatted(normalized));
            }
        }
        return List.copyOf(files);
    }

    private void collectDirectory(Path directory, Set<Path> files) {
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.filter(Files::isRegularFile)
                    .filter(loaderFactory::supports)
                    .filter(path -> !isIgnored(directory, path))
                    .map(path -> path.toAbsolutePath().normalize())
                    .sorted()
                    .forEach(files::add);
        } catch (IOException ex) {
            throw new DocumentLoadException(
                    "No se pudo recorrer la carpeta " + directory + ": " + ex.getMessage(), ex);
        }
    }

    private static boolean isIgnored(Path root, Path path) {
        Path relative = root.relativize(path);
        for (Path segment : relative) {
            if (IGNORED_DIRECTORIES.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }
}
