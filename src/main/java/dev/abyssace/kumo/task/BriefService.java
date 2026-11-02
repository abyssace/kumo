package dev.abyssace.kumo.task;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.abyssace.kumo.chunk.TokenEstimator;
import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.config.BriefTemplate;
import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.llm.StructuredLlmClient;
import dev.abyssace.kumo.prompt.BriefPromptBuilder;
import dev.abyssace.kumo.summarize.SummaryLanguage;
import dev.abyssace.kumo.summarize.SummaryListener;
import dev.abyssace.kumo.summarize.SummaryService;
import dev.abyssace.kumo.summarize.SummaryStrategy;
import java.io.IOException;
import java.util.Objects;

/** Patrón: Facade + inyección por constructor. Extrae, valida y conserva procedencia sin usar índices. */
public final class BriefService {
    private final StructuredLlmClient llm;
    private final SummaryService summaries;
    private final PromptBudget budget;
    private final ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    public BriefService(StructuredLlmClient llm, TokenEstimator estimator, AppConfig config) {
        this.llm = Objects.requireNonNull(llm, "llm");
        summaries = new SummaryService(llm, estimator, config);
        budget = new PromptBudget(estimator, config);
    }

    public JsonNode extract(Document document, BriefTemplate template, SummaryLanguage language,
            boolean sourceCode, SummaryListener listener) {
        BriefSchema schemas = new BriefSchema();
        JsonNode schema = schemas.create(template, sourceCode);
        String format = schema.toString();
        BriefPromptBuilder prompts = new BriefPromptBuilder();
        // Detecta una plantilla demasiado grande antes de resumir el documento.
        budget.require(prompts.build("", template, language, sourceCode), format, "");
        String original = document.text();
        String content = original;
        String prompt = prompts.build(content, template, language, sourceCode);
        boolean compressed = !budget.fits(prompt, format, content);
        if (compressed) {
            listener.onProgress(Messages.BRIEF_COMPRESSION, 1, 1);
            content = summaries.summarize(document, SummaryStrategy.AUTO, language, sourceCode, listener,
                    BriefPromptBuilder.EXTRACTION_FOCUS + template.instructions());
            prompt = prompts.build(content, template, language, sourceCode);
        }
        budget.require(prompt, format, content);
        listener.onProgress(Messages.BRIEF_EXTRACT, 1, 1);
        String raw = llm.completeStructured(prompt, format);
        listener.onProgress(Messages.BRIEF_VALIDATE, 1, 1);
        JsonNode brief;
        try {
            brief = mapper.readTree(raw == null ? "" : raw);
        } catch (IOException ex) {
            throw new TaskException(Messages.BRIEF_JSON_INVALID.formatted(ex.getMessage()));
        }
        schemas.validate(brief, schema);
        validateTaskEvidence(brief, original);
        var result = mapper.createObjectNode();
        result.put("source", document.path().toString());
        result.put("compressed", compressed);
        result.putArray("warnings").add(Messages.BRIEF_WARNING);
        if (compressed) {
            result.withArray("warnings").add(Messages.BRIEF_COMPRESSION);
        }
        result.set("brief", brief);
        return result;
    }

    private void validateTaskEvidence(JsonNode brief, String source) {
        String normalized = normalize(source);
        for (JsonNode task : brief.path("tasks")) {
            for (String field : new String[] {"evidence", "assignee", "dueDate"}) {
                JsonNode value = task.get(field);
                if (!value.isNull() && !normalized.contains(normalize(value.asText()))) {
                    throw new TaskException(Messages.BRIEF_EVIDENCE_ERROR);
                }
            }
        }
    }

    private String normalize(String text) {
        return text.strip().replaceAll("\\s+", " ");
    }
}
