package dev.abyssace.kumo.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EmbeddedChunkTest {

    @Test
    void copiaElVectorDeEntrada() {
        float[] original = {1, 2, 3};
        EmbeddedChunk embedded = new EmbeddedChunk(chunk(), original);

        original[0] = 99;

        assertArrayEquals(new float[] {1, 2, 3}, embedded.vector());
    }

    @Test
    void copiaElVectorDeSalida() {
        EmbeddedChunk embedded = new EmbeddedChunk(chunk(), new float[] {1, 2, 3});

        float[] returned = embedded.vector();
        returned[0] = 99;

        assertArrayEquals(new float[] {1, 2, 3}, embedded.vector());
    }

    @Test
    void rechazaVectorVacio() {
        assertThrows(IllegalArgumentException.class, () -> new EmbeddedChunk(chunk(), new float[0]));
    }

    private static Chunk chunk() {
        return Chunk.of(Path.of("docs", "a.txt"), 0, "texto", SourceLocation.none());
    }
}
