package dev.abyssace.kumo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CitationTest {

    @Test
    void recortaElExtractoYColapsaEspacios() {
        Chunk chunk = Chunk.of(
                Path.of("docs", "a.txt"),
                2,
                "  uno   dos\ntres cuatro  ",
                new SourceLocation.LineRange(4, 5));
        ScoredChunk scored = new ScoredChunk(new EmbeddedChunk(chunk, new float[] {1}), 0.5);

        Citation citation = Citation.from(scored, 10);

        assertEquals("uno dos tr…", citation.excerpt());
        assertEquals("a.txt", citation.source().getFileName().toString());
        assertEquals(2, citation.chunkIndex());
        assertEquals(new SourceLocation.LineRange(4, 5), citation.location());
        assertEquals(0.5, citation.score(), 1e-9);
    }

    @Test
    void noAgregaPuntosSuspensivosSiCabeCompleto() {
        Chunk chunk = Chunk.of(Path.of("docs", "a.txt"), 0, "uno dos", SourceLocation.none());
        ScoredChunk scored = new ScoredChunk(new EmbeddedChunk(chunk, new float[] {1}), 1.0);

        assertEquals("uno dos", Citation.from(scored, 20).excerpt());
    }

    @Test
    void rechazaTamanoDeExtractoInvalido() {
        Chunk chunk = Chunk.of(Path.of("docs", "a.txt"), 0, "texto", SourceLocation.none());
        ScoredChunk scored = new ScoredChunk(new EmbeddedChunk(chunk, new float[] {1}), 1.0);

        assertThrows(IllegalArgumentException.class, () -> Citation.from(scored, 0));
    }
}
