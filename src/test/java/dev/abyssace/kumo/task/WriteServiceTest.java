package dev.abyssace.kumo.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.chunk.HeuristicTokenEstimator;
import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.EmbeddedChunk;
import dev.abyssace.kumo.domain.SourceLocation;
import dev.abyssace.kumo.fake.FakeEmbedder;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.store.CosineSimilarity;
import dev.abyssace.kumo.store.InMemoryVectorStore;
import dev.abyssace.kumo.summarize.SummaryLanguage;
import dev.abyssace.kumo.summarize.SummaryListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WriteServiceTest {
    private final FakeEmbedder embedder = new FakeEmbedder();
    private final InMemoryVectorStore store = new InMemoryVectorStore(new CosineSimilarity());
    private final List<String> prompts = new ArrayList<>();
    private final List<String> stages = new ArrayList<>();
    private String response = "## Introducción\nKumo usa Ollama local [1].";
    private final SummaryListener listener = new SummaryListener() {
        public void onProgress(String stage, int current, int total) { stages.add(stage); }
    };

    @Test
    void recuperaTopKMayorYDevuelveFuentesConUbicacionYExtractos() {
        for (int i = 0; i < 15; i++) { add(i, "Kumo documento " + i); }
        var answer = service(AppConfig.defaults()).write("Kumo", SummaryLanguage.DOCUMENT, 12, listener);
        assertEquals(12, answer.citations().size());
        assertTrue(answer.citations().stream().allMatch(c -> !c.excerpt().isBlank()));
        assertTrue(prompts.getFirst().contains("[12]"));
        assertTrue(prompts.getFirst().contains("idioma de la instrucción"));
        assertEquals(List.of(Messages.WRITE_RETRIEVE, Messages.WRITE_GENERATE), stages);
    }

    @Test
    void respetaIdiomaYNumeraSoloElContextoEntregado() {
        add(0, "Kumo usa Ollama");
        service(AppConfig.defaults()).write("Introduce Kumo", SummaryLanguage.ENGLISH, 3, listener);
        assertTrue(prompts.getFirst().contains("Write in English"));
        assertTrue(prompts.getFirst().contains("no afirmes cobertura completa"));
    }

    @Test
    void rechazaIndiceVacioSinConsultarElModelo() {
        assertEquals(Messages.INDEX_EMPTY, assertThrows(TaskException.class,
                () -> service(AppConfig.defaults()).write("Kumo", SummaryLanguage.DOCUMENT, 12, listener)).getMessage());
        assertTrue(prompts.isEmpty());
    }

    @Test
    void respetaMinScoreYNoGeneraSinFuentesRelevantes() {
        add(0, "perro jardín");
        assertEquals(Messages.WRITE_NO_CONTEXT, assertThrows(TaskException.class,
                () -> service(AppConfig.builder().minScore(0.9).build())
                        .write("Kumo", SummaryLanguage.DOCUMENT, 12, listener)).getMessage());
        assertTrue(prompts.isEmpty());
    }

    @Test
    void rechazaContextoOInstruccionGrandesSinTruncar() {
        add(0, "Kumo " + "x".repeat(40000));
        assertThrows(TaskException.class,
                () -> service(AppConfig.defaults()).write("Kumo", SummaryLanguage.DOCUMENT, 12, listener));
        assertTrue(prompts.isEmpty());
        store.clear();
        add(1, "Kumo pequeño");
        assertThrows(TaskException.class, () -> service(AppConfig.defaults())
                .write("Kumo " + "instrucción larga ".repeat(3000), SummaryLanguage.DOCUMENT, 12, listener));
        assertTrue(prompts.isEmpty());
    }

    @Test
    void rechazaRespuestasVaciasSinCitasOCitasInexistentes() {
        add(0, "Kumo local");
        for (String text : List.of("", "sin citas", "dato [0]", "dato [2]", "dato [999999999999999999999999]")) {
            response = text;
            assertThrows(TaskException.class,
                    () -> service(AppConfig.defaults()).write("Kumo", SummaryLanguage.DOCUMENT, 12, listener));
        }
    }

    @Test
    void validaInstruccionYTopK() {
        var service = service(AppConfig.defaults());
        assertThrows(IllegalArgumentException.class,
                () -> service.write("  ", SummaryLanguage.DOCUMENT, 12, listener));
        assertThrows(IllegalArgumentException.class,
                () -> service.write("Kumo", SummaryLanguage.DOCUMENT, 0, listener));
        assertTrue(prompts.isEmpty());
    }

    private void add(int number, String text) {
        var chunk = Chunk.of(Path.of("input.pdf"), number, text, new SourceLocation.PageRef(number + 1));
        store.add(List.of(new EmbeddedChunk(chunk, embedder.embed(text))));
    }

    private WriteService service(AppConfig config) {
        return new WriteService(embedder, store, new dev.abyssace.kumo.llm.LlmClient() {
            public String complete(String prompt) { prompts.add(prompt); return response; }
            public void stream(String prompt, java.util.function.Consumer<String> consumer) {
                throw new AssertionError("write no usa streaming");
            }
        }, config, new HeuristicTokenEstimator());
    }
}
