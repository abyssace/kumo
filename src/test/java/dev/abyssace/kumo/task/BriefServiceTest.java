package dev.abyssace.kumo.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.chunk.HeuristicTokenEstimator;
import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.config.BriefTemplate;
import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.domain.DocumentFormat;
import dev.abyssace.kumo.domain.DocumentPage;
import dev.abyssace.kumo.fake.BriefFixtures;
import dev.abyssace.kumo.fake.FakeStructuredLlmClient;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.summarize.SummaryLanguage;
import dev.abyssace.kumo.summarize.SummaryException;
import dev.abyssace.kumo.summarize.SummaryListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BriefServiceTest {
    private final FakeStructuredLlmClient llm = new FakeStructuredLlmClient();
    private final List<String> stages = new ArrayList<>();
    private final SummaryListener listener = new SummaryListener() {
        @Override
        public void onProgress(String stage, int current, int total) { stages.add(stage); }
    };

    @Test
    void extraeConEsquemaYProcedenciaSinResumirSiCabe() {
        var result = extract("Kumo usa Ollama local.", BriefTemplate.defaults(), AppConfig.defaults());
        assertEquals(BriefFixtures.valid(), result.get("brief"));
        assertFalse(result.path("compressed").asBoolean());
        assertEquals(Path.of("input.txt").toAbsolutePath().toString(), result.path("source").asText());
        assertEquals(0, llm.summaries);
        assertTrue(llm.schemas.getFirst().contains("additionalProperties"));
        assertTrue(llm.prompts.getFirst().contains("no tareas asignadas"));
        assertTrue(stages.contains(Messages.BRIEF_VALIDATE));
    }

    @Test
    void aplicaLosCamposElegidosEnLaPlantilla() {
        var template = new BriefTemplate(List.of("title", "summary"), "Solo lo esencial.");
        llm.response = "{\"title\":\"kumo\",\"summary\":\"Local\"}";
        var result = extract("texto", template, AppConfig.defaults());
        assertEquals(2, result.get("brief").size());
        assertFalse(llm.schemas.getFirst().contains("scientific"));
        assertTrue(llm.prompts.getFirst().contains("Solo lo esencial."));
    }

    @Test
    void rechazaJsonRotoCamposAusentesExtrasYTiposErroneos() {
        var broken = BriefFixtures.valid();
        broken.put("tasks", "no es una lista");
        var extra = BriefFixtures.valid().put("inventado", "dato");
        for (String response : List.of("{ roto", "null", "[]", "{}", "```json\n{}\n```",
                broken.toString(), extra.toString(), BriefFixtures.valid() + " {}",
                "{\"title\":\"a\",\"title\":\"b\"}")) {
            llm.response = response;
            assertThrows(TaskException.class, () -> extract("texto", BriefTemplate.defaults(), AppConfig.defaults()));
        }
    }

    @Test
    void validaObjetosCientificosYTecnicosYTiposAnidados() throws Exception {
        var value = BriefFixtures.valid();
        value.put("documentType", "scientific");
        var science = value.putObject("scientific");
        science.put("objective", "Evaluar un método").putNull("method");
        science.putArray("results").add("Resultado descrito");
        science.putArray("limitations");
        var technical = value.putObject("technical");
        technical.putNull("purpose");
        technical.putArray("components");
        technical.putArray("flow");
        technical.putArray("dependencies");
        llm.response = value.toString();
        extract("texto", BriefTemplate.defaults(), AppConfig.defaults());
        science.withArray("results").add(3);
        llm.response = value.toString();
        assertThrows(TaskException.class, () -> extract("texto", BriefTemplate.defaults(), AppConfig.defaults()));
    }

    @Test
    void aceptaTareasConEvidenciaLiteralYDatosAusentesNulos() {
        var value = BriefFixtures.valid();
        value.withArray("tasks").addObject().put("description", "Revisar el informe")
                .putNull("assignee").putNull("dueDate").put("evidence", "Revisar el informe");
        llm.response = value.toString();
        assertEquals(1, extract("Tarea: Revisar el\ninforme", BriefTemplate.defaults(), AppConfig.defaults())
                .path("brief").path("tasks").size());
    }

    @Test
    void rechazaEvidenciaResponsablesOFechasInventadas() {
        for (String field : List.of("evidence", "assignee", "dueDate")) {
            var value = BriefFixtures.valid();
            var task = value.withArray("tasks").addObject().put("description", "Revisar")
                    .putNull("assignee").putNull("dueDate").put("evidence", "Revisar");
            task.put(field, "DATO_INVENTADO");
            llm.response = value.toString();
            assertEquals(Messages.BRIEF_EVIDENCE_ERROR, assertThrows(TaskException.class,
                    () -> extract("Revisar el informe", BriefTemplate.defaults(), AppConfig.defaults())).getMessage());
        }
    }

    @Test
    void comprimeDocumentosLargosConAdvertenciaYFocoDeExtraccion() {
        var result = extract("Información importante. ".repeat(3000), BriefTemplate.defaults(), AppConfig.defaults());
        assertTrue(result.path("compressed").asBoolean());
        assertTrue(llm.summaries > 1);
        assertTrue(stages.contains(Messages.BRIEF_COMPRESSION));
        assertEquals(2, result.path("warnings").size());
    }

    @Test
    void noTruncaUnaSintesisQueSigueSinCaber() {
        llm.summary = "s".repeat(30000);
        assertThrows(SummaryException.class,
                () -> extract("x".repeat(60000), BriefTemplate.defaults(), AppConfig.defaults()));
        assertTrue(llm.schemas.isEmpty());
    }

    @Test
    void rechazaUnContextoInsuficienteAntesDeGenerar() {
        assertThrows(TaskException.class, () -> extract("texto", BriefTemplate.defaults(),
                AppConfig.builder().numCtx(512).build()));
        assertTrue(llm.schemas.isEmpty());
        assertEquals(0, llm.summaries);
    }

    @Test
    void respetaIdiomaYCodigoEnLaExtraccion() {
        llm.response = BriefFixtures.valid().put("documentType", "code").toString();
        var service = new BriefService(llm, new HeuristicTokenEstimator(), AppConfig.defaults());
        service.extract(document("class Test {}"), BriefTemplate.defaults(), SummaryLanguage.ENGLISH, true, listener);
        assertTrue(llm.prompts.getFirst().contains("Write the values in English"));
        assertTrue(llm.prompts.getFirst().contains("No traduzcas identificadores"));
    }

    @Test
    void noConfundeUnDocumentoSobreSoftwareConCodigoFuente() {
        llm.response = BriefFixtures.valid().put("documentType", "code").toString();
        assertThrows(TaskException.class,
                () -> extract("Descripción del proyecto kumo", BriefTemplate.defaults(), AppConfig.defaults()));
    }

    private com.fasterxml.jackson.databind.JsonNode extract(String text, BriefTemplate template, AppConfig config) {
        return new BriefService(llm, new HeuristicTokenEstimator(), config)
                .extract(document(text), template, SummaryLanguage.DOCUMENT, false, listener);
    }

    private Document document(String text) {
        return new Document(Path.of("input.txt"), "input", DocumentFormat.TXT, List.of(new DocumentPage(1, text)));
    }
}
