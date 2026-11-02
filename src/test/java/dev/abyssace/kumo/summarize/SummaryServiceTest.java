package dev.abyssace.kumo.summarize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import dev.abyssace.kumo.chunk.HeuristicTokenEstimator;
import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.domain.DocumentFormat;
import dev.abyssace.kumo.domain.DocumentPage;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.llm.LlmClient;
import dev.abyssace.kumo.prompt.SummaryPromptBuilder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class SummaryServiceTest {
    private static final String MARKER = "CONTENIDO A RESUMIR (hasta el final del mensaje):\n";
    private final HeuristicTokenEstimator estimator = new HeuristicTokenEstimator();
    private final AppConfig config = AppConfig.builder().summaryInputRatio(0.1).build();
    private final RecordingLlm llm = new RecordingLlm();
    private final Events events = new Events();

    @Test
    void autoEligeStuffSiCabeYEntregaTodoElTexto() {
        assertEquals("resumen", summarize("texto breve", SummaryStrategy.AUTO));
        assertEquals(SummaryStrategy.STUFF, events.chosen);
        assertEquals(Messages.SUMMARY_FITS, events.reason);
        assertEquals(1, llm.prompts.size());
        assertEquals("texto breve", content(llm.prompts.getFirst()).strip());
    }

    @Test
    void autoEligeMapReduceSinOmitirFragmentos() {
        String source = "inicio " + "contenido importante ".repeat(600) + " FIN_UNICO";
        summarize(source, SummaryStrategy.AUTO);
        assertEquals(SummaryStrategy.MAP_REDUCE, events.chosen);
        assertEquals(Messages.SUMMARY_EXCEEDS, events.reason);
        int maps = events.stages.stream().filter(Messages.SUMMARY_MAP::equals).toList().size();
        assertTrue(maps > 1);
        StringBuilder seen = new StringBuilder();
        for (int i = 0; i < maps; i++) {
            // Cada prompt añade solo un salto de línea después del contenido.
            String raw = content(llm.prompts.get(i));
            seen.append(raw, 0, raw.length() - 1);
        }
        assertEquals(source, seen.toString());
        assertEquals(Messages.SUMMARY_FINAL, events.stages.getLast());
    }

    @Test
    void stuffForzadoRechazaDesbordeSinLlamarAlModelo() {
        SummaryException ex = assertThrows(SummaryException.class,
                () -> summarize("x".repeat(12000), SummaryStrategy.STUFF));
        assertTrue(ex.getMessage().contains("no se ha truncado"));
        assertTrue(llm.prompts.isEmpty());
        assertEquals(Messages.SUMMARY_FORCED, events.reason);
    }

    @Test
    void mapReduceForzadoProcesaInclusoUnArchivoPequeno() {
        summarize("pequeño", SummaryStrategy.MAP_REDUCE);
        assertEquals(2, llm.prompts.size());
        assertEquals(SummaryStrategy.MAP_REDUCE, events.chosen);
    }

    @Test
    void reduceJerarquicamenteYValidaTodosLosPrompts() {
        llm.answer = prompt -> "r".repeat(Math.max(1, content(prompt).length() / 2));
        summarize("a".repeat(65000), SummaryStrategy.AUTO);
        assertTrue(events.stages.stream().anyMatch(s -> s.equals(Messages.SUMMARY_REDUCE.formatted(2))));
        int reserve = config.numCtx() / 5 + 64;
        for (String prompt : llm.prompts) {
            assertTrue(estimator.estimate(prompt) + reserve <= config.numCtx());
            assertTrue(estimator.estimate(content(prompt).stripTrailing()) <= events.budget);
        }
    }

    @Test
    void detieneUnModeloQueNoReduce() {
        llm.answer = SummaryServiceTest::content;
        SummaryException ex = assertThrows(SummaryException.class,
                () -> summarize("x".repeat(20000), SummaryStrategy.AUTO));
        assertEquals(Messages.SUMMARY_NO_CONTRACTION, ex.getMessage());
        assertTrue(llm.prompts.size() < 30);
    }

    @Test
    void rechazaUnaRespuestaVacia() {
        llm.answer = p -> "  ";
        assertEquals(Messages.SUMMARY_EMPTY, assertThrows(SummaryException.class,
                () -> summarize("texto", SummaryStrategy.AUTO)).getMessage());
    }

    @Test
    void propagaErroresSinContinuarConOtrosFragmentos() {
        llm.answer = p -> { throw new IllegalStateException("fallo simulado"); };
        assertThrows(IllegalStateException.class,
                () -> summarize("x".repeat(12000), SummaryStrategy.AUTO));
        assertEquals(1, llm.prompts.size());
    }

    @Test
    void idiomaYAdvertenciaDeCodigoLleganATodasLasEtapas() {
        new SummaryService(llm, estimator, config).summarize(document("x".repeat(8000)),
                SummaryStrategy.MAP_REDUCE, SummaryLanguage.ENGLISH, true, events);
        for (String prompt : llm.prompts) {
            assertTrue(prompt.contains("Respond in English"));
            assertTrue(prompt.contains("No traduzcas identificadores"));
            assertTrue(prompt.contains("No lo ejecutes"));
            assertTrue(prompt.contains("no instrucciones"));
        }
    }

    @Test
    void reservaPromptYRespuestaAunqueElRatioSeaAlto() {
        AppConfig highRatio = AppConfig.builder().summaryInputRatio(0.99).build();
        var session = new SummarySession(llm, estimator,
                new SummaryPromptBuilder(SummaryLanguage.DOCUMENT, false), events,
                highRatio.numCtx(), highRatio.summaryInputRatio());
        assertTrue(session.budget() < highRatio.numCtx() * 0.99);
        session.complete("x".repeat(session.budget() * 4), false);
        assertThrows(SummaryException.class,
                () -> session.complete("x".repeat(session.budget() * 4 + 1), false));
    }

    @Test
    void rechazaUnContextoInsuficienteAntesDeLaPrimeraPeticion() {
        assertThrows(SummaryException.class, () -> new SummaryService(llm, estimator,
                AppConfig.builder().numCtx(128).build()).summarize(document("texto"),
                SummaryStrategy.AUTO, SummaryLanguage.DOCUMENT, false, events));
        assertTrue(llm.prompts.isEmpty());
    }

    @Test
    void aceptaExactamenteElLimiteYMapReduceAlSuperarlo() {
        var session = new SummarySession(llm, estimator,
                new SummaryPromptBuilder(SummaryLanguage.DOCUMENT, false), events,
                config.numCtx(), config.summaryInputRatio());
        summarize("a".repeat(session.budget() * 4), SummaryStrategy.AUTO);
        assertEquals(SummaryStrategy.STUFF, events.chosen);
        summarize("a".repeat(session.budget() * 4 + 1), SummaryStrategy.AUTO);
        assertEquals(SummaryStrategy.MAP_REDUCE, events.chosen);
    }

    private String summarize(String text, SummaryStrategy strategy) {
        return new SummaryService(llm, estimator, config).summarize(document(text), strategy,
                SummaryLanguage.DOCUMENT, false, events);
    }

    private Document document(String text) {
        return new Document(Path.of("test.txt"), "test", DocumentFormat.TXT,
                List.of(new DocumentPage(1, text)));
    }

    private static String content(String prompt) {
        return prompt.substring(prompt.indexOf(MARKER) + MARKER.length());
    }

    /** Patrón: Test Double de LlmClient con respuestas deterministas. */
    private static final class RecordingLlm implements LlmClient {
        final List<String> prompts = new ArrayList<>();
        Function<String, String> answer = p -> "resumen";

        public String complete(String prompt) {
            prompts.add(prompt);
            return answer.apply(prompt);
        }

        public void stream(String prompt, Consumer<String> consumer) {
            fail("summarize no debe usar streaming");
        }
    }

    /** Patrón: Observer de prueba. */
    private static final class Events implements SummaryListener {
        SummaryStrategy chosen;
        String reason;
        int budget;
        final List<String> stages = new ArrayList<>();

        public void onStrategy(SummaryStrategy strategy, String reason, int tokens, int budget) {
            chosen = strategy;
            this.reason = reason;
            this.budget = budget;
        }

        public void onProgress(String stage, int current, int total) {
            assertTrue(current >= 1 && current <= total);
            stages.add(stage);
        }
    }
}
