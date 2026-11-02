package dev.abyssace.kumo.rag;

import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.EmbeddedChunk;
import dev.abyssace.kumo.embed.Embedder;
import dev.abyssace.kumo.store.VectorStore;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Patrón: Domain Service + Strategy.
 *
 * <p>Convierte fragmentos en vectores y los guarda. Centraliza la validación
 * del número de vectores para que {@link RagService} e {@link IndexingService}
 * no dupliquen la misma lógica. {@code replace} sustituye los fragmentos de un
 * archivo concreto, que es lo que necesita {@code add} al reindexar.</p>
 */
public final class ChunkIndexer {

    private final Embedder embedder;
    private final VectorStore store;

    public ChunkIndexer(Embedder embedder, VectorStore store) {
        this.embedder = Objects.requireNonNull(embedder, "embedder");
        this.store = Objects.requireNonNull(store, "store");
    }

    /** Embebe y añade los fragmentos. Devuelve cuántos se procesaron. */
    public int index(List<Chunk> chunks) {
        Objects.requireNonNull(chunks, "chunks");
        List<EmbeddedChunk> embedded = embed(chunks);
        if (embedded.isEmpty()) {
            return 0;
        }
        store.add(embedded);
        return embedded.size();
    }

    /**
     * Embebe los fragmentos y sustituye los que hubiera del mismo archivo.
     * Devuelve cuántos se procesaron.
     */
    public int replace(Path source, List<Chunk> chunks) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(chunks, "chunks");
        List<EmbeddedChunk> embedded = embed(chunks);
        store.replaceBySource(source, embedded);
        return embedded.size();
    }

    /** Embebe sin guardar; útil para tests y para operaciones por lotes. */
    public List<EmbeddedChunk> embed(List<Chunk> chunks) {
        Objects.requireNonNull(chunks, "chunks");
        if (chunks.isEmpty()) {
            return List.of();
        }

        List<String> texts = chunks.stream().map(Chunk::text).toList();
        List<float[]> vectors = embedder.embedAll(texts);
        Objects.requireNonNull(vectors, "vectors");
        if (vectors.size() != chunks.size()) {
            throw new IllegalStateException(
                    "El embedder devolvió " + vectors.size() + " vectores para " + chunks.size() + " fragmentos");
        }

        List<EmbeddedChunk> embedded = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            embedded.add(new EmbeddedChunk(chunks.get(i), vectors.get(i)));
        }
        return List.copyOf(embedded);
    }
}
