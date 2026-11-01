package dev.abyssace.kumo.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.domain.Answer;
import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.Citation;
import dev.abyssace.kumo.domain.SourceLocation;
import dev.abyssace.kumo.embed.Embedder;
import dev.abyssace.kumo.fake.EchoLlmClient;
import dev.abyssace.kumo.fake.FakeEmbedder;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.prompt.RagPromptBuilder;
import dev.abyssace.kumo.store.CosineSimilarity;
import dev.abyssace.kumo.store.InMemoryVectorStore;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RagServiceTest {

    private FakeEmbedder embedder;
    private InMemoryVectorStore store;
    private EchoLlmClient llm;
    private RagService rag;

    @BeforeEach
    void setUp() {
        embedder = new FakeEmbedder();
        store = new InMemoryVectorStore(new CosineSimilarity());
        llm = new EchoLlmClient();
        rag = new RagService(embedder, store, llm, new RagPromptBuilder(), 5, -1.0);
    }

    @Test
    void indexaYDevuelveCitasConUbicacion() {
        int indexed = rag.index(List.of(
                chunk("docs/informe.pdf", 0, "gato", new SourceLocation.PageRef(2)),
                chunk("docs/notas.txt", 3, "perro", new SourceLocation.LineRange(10, 12))));

        assertEquals(2, indexed);
        assertEquals(2, store.size());

        Answer answer = rag.ask("gato");

        assertEquals(EchoLlmClient.RESPONSE, answer.text());
        assertEquals(2, answer.citations().size());
        assertTrue(answer.hasCitations());

        Citation first = answer.citations().get(0);
        assertEquals("informe.pdf", first.source().getFileName().toString());
        assertEquals(new SourceLocation.PageRef(2), first.location());
        assertTrue(first.excerpt().contains("gato"));
        assertEquals(1.0, first.score(), 1e-6);
    }

    @Test
    void elPromptIncluyeFuentesNumeradasYPregunta() {
        rag.index(List.of(chunk("docs/informe.pdf", 0, "gato", new SourceLocation.PageRef(7))));

        rag.ask("¿Dónde duerme el gato?");

        String prompt = llm.lastPrompt();
        assertTrue(prompt.contains("[1] archivo: informe.pdf | página 7"));
        assertTrue(prompt.contains("¿Dónde duerme el gato?"));
        assertTrue(prompt.contains("mismo idioma"));
    }

    @Test
    void sinFuentesNoLlamaAlModelo() {
        Answer answer = rag.ask("gato");

        assertEquals(Messages.NO_CONTEXT, answer.text());
        assertFalse(answer.hasCitations());
        assertTrue(llm.prompts().isEmpty());
    }

    @Test
    void minScoreFiltraResultados() {
        rag.index(List.of(
                chunk("docs/a.txt", 0, "gato", SourceLocation.none()),
                chunk("docs/b.txt", 0, "perro", SourceLocation.none())));

        RagService exigente = new RagService(embedder, store, llm, new RagPromptBuilder(), 5, 0.99);
        Answer answer = exigente.ask("gato");

        assertEquals(1, answer.citations().size());
        assertTrue(answer.citations().get(0).excerpt().contains("gato"));
    }

    @Test
    void topKLimitaLasCitas() {
        rag.index(List.of(
                chunk("docs/a.txt", 0, "gato", SourceLocation.none()),
                chunk("docs/b.txt", 0, "gato gato", SourceLocation.none()),
                chunk("docs/c.txt", 0, "gato gato gato", SourceLocation.none())));

        RagService unSoloFragmento = new RagService(embedder, store, llm, new RagPromptBuilder(), 1, -1.0);

        assertEquals(1, unSoloFragmento.ask("gato").citations().size());
    }

    @Test
    void streamingNotificaYAcumulaLaRespuesta() {
        rag.index(List.of(chunk("docs/a.txt", 0, "gato", SourceLocation.none())));
        StringBuilder sink = new StringBuilder();

        Answer answer = rag.askStreaming("gato", sink::append);

        assertEquals(EchoLlmClient.RESPONSE, answer.text());
        assertEquals(EchoLlmClient.RESPONSE, sink.toString());
        assertEquals(1, answer.citations().size());
    }

    @Test
    void fallaSiElEmbedderDevuelveUnNumeroIncorrectoDeVectores() {
        Embedder incompleto = new Embedder() {
            @Override
            public float[] embed(String text) {
                return new float[] {1, 0};
            }

            @Override
            public List<float[]> embedAll(List<String> texts) {
                return List.of(new float[] {1, 0});
            }
        };
        RagService servicio = new RagService(incompleto, store, llm, new RagPromptBuilder(), 5, -1.0);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> servicio.index(List.of(
                        chunk("docs/a.txt", 0, "uno", SourceLocation.none()),
                        chunk("docs/b.txt", 0, "dos", SourceLocation.none()))));

        assertTrue(error.getMessage().contains("1"));
        assertTrue(error.getMessage().contains("2"));
    }

    @Test
    void preguntaVaciaFalla() {
        assertThrows(IllegalArgumentException.class, () -> rag.ask("   "));
    }

    private static Chunk chunk(String source, int index, String text, SourceLocation location) {
        return Chunk.of(Path.of(source), index, text, location);
    }
}
