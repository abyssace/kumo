package dev.abyssace.kumo.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.EmbeddedChunk;
import dev.abyssace.kumo.domain.ScoredChunk;
import dev.abyssace.kumo.domain.SourceLocation;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryVectorStoreTest {

    private InMemoryVectorStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryVectorStore(new CosineSimilarity());
    }

    @Test
    void buscaOrdenadoPorSimilitud() {
        store.add(List.of(
                chunk("a", 1, 0),
                chunk("b", 0.8f, 0.2f),
                chunk("c", 0, 1)));

        List<ScoredChunk> results = store.search(new float[] {1, 0}, 3);

        assertEquals(3, results.size());
        assertEquals("a", results.get(0).embedded().chunk().id());
        assertEquals("b", results.get(1).embedded().chunk().id());
        assertEquals("c", results.get(2).embedded().chunk().id());
        assertTrue(results.get(0).score() >= results.get(1).score());
        assertTrue(results.get(1).score() >= results.get(2).score());
    }

    @Test
    void topKLimitaLosResultados() {
        store.add(List.of(chunk("a", 1, 0), chunk("b", 0.5f, 0.5f)));

        assertEquals(1, store.search(new float[] {1, 0}, 1).size());
        assertEquals(0, store.search(new float[] {1, 0}, 0).size());
        assertEquals(0, store.search(new float[] {1, 0}, -3).size());
    }

    @Test
    void reemplazaFragmentosConElMismoId() {
        store.add(List.of(chunk("a", 1, 0)));
        store.add(List.of(chunk("a", 0, 1)));

        assertEquals(1, store.size());
        assertEquals(1.0, store.search(new float[] {0, 1}, 1).get(0).score(), 1e-9);
    }

    @Test
    void rechazaDimensionesIncompatiblesSinDejarElStoreAMedias() {
        store.add(List.of(chunk("a", 1, 0)));

        assertThrows(
                IllegalArgumentException.class,
                () -> store.add(List.of(chunk("b", 1, 0), chunk("c", 1, 0, 0))));

        assertEquals(1, store.size());
    }

    @Test
    void consultaConDimensionIncorrectaFalla() {
        store.add(List.of(chunk("a", 1, 0)));

        assertThrows(IllegalArgumentException.class, () -> store.search(new float[] {1, 0, 0}, 1));
    }

    @Test
    void limpiarDejaElStoreVacio() {
        store.add(List.of(chunk("a", 1, 0)));

        store.clear();

        assertEquals(0, store.size());
        assertTrue(store.search(new float[] {1, 0}, 5).isEmpty());
    }

    @Test
    void metricaNulaFalla() {
        assertThrows(NullPointerException.class, () -> new InMemoryVectorStore(null));
    }

    private static EmbeddedChunk chunk(String id, float... vector) {
        Chunk chunk = new Chunk(id, Path.of("docs", "doc.txt"), 0, "texto " + id, SourceLocation.none());
        return new EmbeddedChunk(chunk, vector);
    }
}
