package dev.abyssace.kumo.chunk;

import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.Document;
import java.util.List;

/**
 * Patrón: Strategy.
 *
 * <p>Divide un documento en fragmentos. La implementación concreta (F2) decide
 * tamaño y solapamiento; el resto del sistema no lo sabe.</p>
 */
public interface Chunker {

    List<Chunk> split(Document document);
}
