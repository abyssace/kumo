package dev.abyssace.kumo.store;

import dev.abyssace.kumo.domain.EmbeddedChunk;
import dev.abyssace.kumo.domain.ScoredChunk;
import java.util.List;

/**
 * Patrón: Repository / Strategy.
 *
 * <p>Almacén de vectores intercambiable. F1 ofrece la implementación en
 * memoria; F2 añadirá la persistencia en JSON sin tocar a los consumidores.
 * {@code size()} es una extensión sobre el contrato mínimo, necesaria para el
 * comando {@code status}.</p>
 */
public interface VectorStore {

    void add(List<EmbeddedChunk> chunks);

    List<ScoredChunk> search(float[] query, int topK);

    void clear();

    int size();
}
