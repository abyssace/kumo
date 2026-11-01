package dev.abyssace.kumo.store;

import dev.abyssace.kumo.domain.EmbeddedChunk;
import dev.abyssace.kumo.domain.ScoredChunk;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Patrón: Repository en memoria + Strategy (métrica inyectada).
 *
 * <p>Guarda los fragmentos por identificador estable: reindexar un archivo
 * reemplaza sus fragmentos en lugar de duplicarlos. No es seguro para uso
 * concurrente; la CLI es de un solo hilo y la futura interfaz web deberá
 * sincronizar el acceso.</p>
 */
public final class InMemoryVectorStore implements VectorStore {

    private final SimilarityMetric metric;
    private final Map<String, EmbeddedChunk> chunks = new LinkedHashMap<>();
    private int dimensions = -1;

    public InMemoryVectorStore(SimilarityMetric metric) {
        this.metric = Objects.requireNonNull(metric, "metric");
    }

    @Override
    public void add(List<EmbeddedChunk> newChunks) {
        Objects.requireNonNull(newChunks, "chunks");

        // Validar todo antes de modificar el store para no dejarlo a medias.
        int expectedDimensions = dimensions;
        for (EmbeddedChunk chunk : newChunks) {
            Objects.requireNonNull(chunk, "chunk");
            int size = chunk.vector().length;
            if (expectedDimensions == -1) {
                expectedDimensions = size;
            } else if (size != expectedDimensions) {
                throw new IllegalArgumentException(
                        "Dimensión incompatible en " + chunk.chunk().id()
                                + ": el índice usa " + expectedDimensions + " y el vector trae " + size
                                + ". Reindexa con el mismo modelo de embeddings.");
            }
        }

        for (EmbeddedChunk chunk : newChunks) {
            chunks.put(chunk.chunk().id(), chunk);
        }
        dimensions = expectedDimensions;
    }

    @Override
    public List<ScoredChunk> search(float[] query, int topK) {
        Objects.requireNonNull(query, "query");
        if (topK <= 0 || chunks.isEmpty()) {
            return List.of();
        }
        if (dimensions != -1 && query.length != dimensions) {
            throw new IllegalArgumentException(
                    "La consulta tiene " + query.length + " dimensiones y el índice usa " + dimensions
                            + ". Reindexa con el mismo modelo de embeddings.");
        }

        return chunks.values().stream()
                .map(chunk -> new ScoredChunk(chunk, metric.score(query, chunk.vector())))
                .sorted()
                .limit(topK)
                .toList();
    }

    @Override
    public void clear() {
        chunks.clear();
        dimensions = -1;
    }

    @Override
    public int size() {
        return chunks.size();
    }

    /**
     * Copia ordenada de los fragmentos guardados. La usa
     * {@link JsonVectorStore} para volcar el índice a disco sin exponer el mapa
     * interno.
     */
    List<EmbeddedChunk> snapshot() {
        return List.copyOf(chunks.values());
    }

    /** Dimensión de los vectores guardados, o -1 si el store está vacío. */
    int dimensions() {
        return dimensions;
    }

    /** Expuesto para diagnóstico y pruebas. */
    public SimilarityMetric metric() {
        return metric;
    }
}
