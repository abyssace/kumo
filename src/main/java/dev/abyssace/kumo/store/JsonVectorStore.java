package dev.abyssace.kumo.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.abyssace.kumo.domain.EmbeddedChunk;
import dev.abyssace.kumo.domain.ScoredChunk;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

/**
 * Patrón: Decorator / Repository persistente sobre {@link InMemoryVectorStore}.
 *
 * <p>Añade persistencia sin duplicar la lógica de búsqueda: delega en el store
 * en memoria y guarda el índice completo en un JSON legible cada vez que
 * cambia. La escritura es atómica (archivo temporal + renombrado) para que un
 * corte a mitad no deje un índice corrupto.</p>
 *
 * <p>El archivo vive en {@code <corpus>/.kumo/index.json} y guarda el modelo de
 * embeddings y la dimensión de los vectores. Si el modelo configurado no
 * coincide, se lanza {@link IndexModelMismatchException}: mezclar vectores de
 * modelos distintos no tiene sentido y hay que reindexar.</p>
 */
public final class JsonVectorStore implements VectorStore {

    /** Subcarpeta oculta que kumo crea dentro de cada corpus. */
    public static final String INDEX_DIRECTORY = ".kumo";

    /** Nombre del archivo de índice. */
    public static final String INDEX_FILE_NAME = "index.json";

    private final Path indexFile;
    private final SimilarityMetric metric;
    private final String embeddingModel;
    private final ObjectMapper mapper;
    private final InMemoryVectorStore delegate;

    /**
     * Abre (o crea de forma perezosa) el índice indicado.
     *
     * @param indexFile      ruta completa del {@code index.json}
     * @param metric         métrica de similitud
     * @param embeddingModel modelo con el que se generaron los vectores
     */
    public JsonVectorStore(Path indexFile, SimilarityMetric metric, String embeddingModel) {
        this.indexFile = Objects.requireNonNull(indexFile, "indexFile").toAbsolutePath().normalize();
        this.metric = Objects.requireNonNull(metric, "metric");
        this.embeddingModel = requireText(embeddingModel, "embeddingModel");
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.delegate = new InMemoryVectorStore(metric);
        loadIfPresent();
    }

    /** Ruta estándar del índice para una carpeta de corpus. */
    public static Path indexPath(Path corpusDir) {
        Objects.requireNonNull(corpusDir, "corpusDir");
        return corpusDir.toAbsolutePath().normalize()
                .resolve(INDEX_DIRECTORY)
                .resolve(INDEX_FILE_NAME);
    }

    @Override
    public void add(List<EmbeddedChunk> chunks) {
        Objects.requireNonNull(chunks, "chunks");
        if (chunks.isEmpty()) {
            return;
        }
        delegate.add(chunks);
        persist();
    }

    @Override
    public List<ScoredChunk> search(float[] query, int topK) {
        return delegate.search(query, topK);
    }

    @Override
    public void clear() {
        delegate.clear();
        try {
            Files.deleteIfExists(indexFile);
        } catch (IOException ex) {
            throw new IndexPersistenceException("No se pudo borrar el índice " + indexFile, ex);
        }
    }

    @Override
    public int size() {
        return delegate.size();
    }

    /** Ruta absoluta del archivo que respalda este store. */
    public Path indexFile() {
        return indexFile;
    }

    /** Modelo de embeddings con el que se abrió el índice. */
    public String embeddingModel() {
        return embeddingModel;
    }

    /** Métrica de similitud en uso. */
    public SimilarityMetric metric() {
        return metric;
    }

    private void loadIfPresent() {
        if (!Files.isRegularFile(indexFile)) {
            return;
        }

        IndexFile file;
        try {
            file = mapper.readValue(indexFile.toFile(), IndexFile.class);
        } catch (IOException ex) {
            throw new IndexFormatException(
                    "No se pudo leer el índice " + indexFile + ": " + ex.getMessage()
                            + ". Borra la carpeta " + indexFile.getParent() + " y reindexa el corpus.", ex);
        }

        validate(file);

        List<EmbeddedChunk> chunks = file.chunks().stream()
                .map(IndexFile.StoredChunk::toEmbedded)
                .toList();
        delegate.add(chunks);
    }

    private void validate(IndexFile file) {
        if (file.formatVersion() != IndexFile.CURRENT_FORMAT_VERSION) {
            throw new IndexFormatException(
                    "El índice " + indexFile + " usa el formato " + file.formatVersion()
                            + " y esta versión de kumo entiende el " + IndexFile.CURRENT_FORMAT_VERSION
                            + ". Borra la carpeta " + indexFile.getParent() + " y reindexa el corpus.");
        }
        if (!embeddingModel.equals(file.embeddingModel())) {
            throw new IndexModelMismatchException(
                    "El índice se creó con el modelo de embeddings '" + file.embeddingModel()
                            + "' y la configuración usa '" + embeddingModel + "'. Reindexa el corpus: borra "
                            + indexFile.getParent() + " y vuelve a indexar.");
        }
        if (!file.chunks().isEmpty() && file.dimensions() < 1) {
            throw new IndexFormatException(
                    "El índice " + indexFile + " no declara dimensiones válidas. Reindexa el corpus.");
        }
        for (IndexFile.StoredChunk chunk : file.chunks()) {
            if (chunk.vector().length != file.dimensions()) {
                throw new IndexFormatException(
                        "El fragmento " + chunk.id() + " tiene " + chunk.vector().length
                                + " dimensiones y el índice declara " + file.dimensions()
                                + ". Reindexa el corpus.");
            }
        }
    }

    private void persist() {
        List<IndexFile.StoredChunk> stored = delegate.snapshot().stream()
                .map(IndexFile.StoredChunk::from)
                .toList();
        IndexFile file = new IndexFile(
                IndexFile.CURRENT_FORMAT_VERSION,
                embeddingModel,
                delegate.dimensions(),
                stored);

        Path parent = indexFile.getParent();
        Path temp = indexFile.resolveSibling(indexFile.getFileName() + ".tmp");
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            mapper.writeValue(temp.toFile(), file);
            moveIntoPlace(temp);
        } catch (IOException ex) {
            throw new IndexPersistenceException("No se pudo guardar el índice " + indexFile, ex);
        }
    }

    private void moveIntoPlace(Path temp) throws IOException {
        try {
            Files.move(temp, indexFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, indexFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " no puede estar vacío");
        }
        return trimmed;
    }
}
