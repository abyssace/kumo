package dev.abyssace.kumo.prompt;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.EmbeddedChunk;
import dev.abyssace.kumo.domain.ScoredChunk;
import dev.abyssace.kumo.domain.SourceLocation;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class RagPromptBuilderTest {

    private final RagPromptBuilder builder = new RagPromptBuilder();

    @Test
    void construyePromptConContextoNumeradoYUbicaciones() {
        Chunk primero = Chunk.of(Path.of("docs", "a.txt"), 0, "contenido uno", new SourceLocation.LineRange(3, 5));
        Chunk segundo = Chunk.of(Path.of("docs", "b.pdf"), 1, "contenido dos", new SourceLocation.PageRef(2));
        List<ScoredChunk> context = List.of(
                new ScoredChunk(new EmbeddedChunk(primero, new float[] {1, 0}), 0.9),
                new ScoredChunk(new EmbeddedChunk(segundo, new float[] {1, 0}), 0.8));

        String prompt = builder.buildRagPrompt("¿Qué dice?", context);

        assertTrue(prompt.contains("[1] archivo: a.txt | líneas 3-5"));
        assertTrue(prompt.contains("[2] archivo: b.pdf | página 2"));
        assertTrue(prompt.contains("contenido uno"));
        assertTrue(prompt.contains("contenido dos"));
        assertTrue(prompt.contains("PREGUNTA:\n¿Qué dice?"));
        assertTrue(prompt.contains("mismo idioma"));
        assertTrue(prompt.contains("No inventes información"));
    }

    @Test
    void contextoVacioFalla() {
        assertThrows(IllegalArgumentException.class, () -> builder.buildRagPrompt("¿Qué dice?", List.of()));
    }

    @Test
    void preguntaVaciaFalla() {
        Chunk chunk = Chunk.of(Path.of("docs", "a.txt"), 0, "texto", SourceLocation.none());
        List<ScoredChunk> context =
                List.of(new ScoredChunk(new EmbeddedChunk(chunk, new float[] {1}), 1.0));

        assertThrows(IllegalArgumentException.class, () -> builder.buildRagPrompt("   ", context));
    }
}
