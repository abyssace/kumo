package dev.abyssace.kumo.task;

import dev.abyssace.kumo.chunk.TokenEstimator;
import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.domain.Answer;
import dev.abyssace.kumo.domain.Citation;
import dev.abyssace.kumo.embed.Embedder;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.llm.LlmClient;
import dev.abyssace.kumo.prompt.WritePromptBuilder;
import dev.abyssace.kumo.store.VectorStore;
import dev.abyssace.kumo.summarize.SummaryLanguage;
import dev.abyssace.kumo.summarize.SummaryListener;
import java.util.Objects;
import java.util.regex.Pattern;

/** Patrón: Facade + inyección por constructor. Redacta desde el índice sin reducir fuentes silenciosamente. */
public final class WriteService {
    private static final Pattern CITATION = Pattern.compile("\\[(\\d+)\\]");
    private final Embedder embedder;
    private final VectorStore store;
    private final LlmClient llm;
    private final AppConfig config;
    private final PromptBudget budget;

    public WriteService(Embedder embedder, VectorStore store, LlmClient llm, AppConfig config,
            TokenEstimator estimator) {
        this.embedder = Objects.requireNonNull(embedder, "embedder");
        this.store = Objects.requireNonNull(store, "store");
        this.llm = Objects.requireNonNull(llm, "llm");
        this.config = Objects.requireNonNull(config, "config");
        budget = new PromptBudget(estimator, config);
    }

    public Answer write(String instruction, SummaryLanguage language, int topK, SummaryListener listener) {
        if (instruction == null || instruction.isBlank()) {
            throw new IllegalArgumentException(Messages.WRITE_INSTRUCTION);
        }
        if (topK < 1) {
            throw new IllegalArgumentException(Messages.WRITE_TOP_K_INVALID);
        }
        if (store.size() == 0) {
            throw new TaskException(Messages.INDEX_EMPTY);
        }
        listener.onProgress(Messages.WRITE_RETRIEVE, 1, 1);
        var context = store.search(embedder.embed(instruction), topK).stream()
                .filter(c -> c.score() >= config.minScore()).toList();
        if (context.isEmpty()) {
            throw new TaskException(Messages.WRITE_NO_CONTEXT);
        }
        String content = context.stream().map(c -> c.embedded().chunk().text())
                .collect(java.util.stream.Collectors.joining("\n\n"));
        String prompt = new WritePromptBuilder().build(instruction, context, language);
        budget.require(prompt, "", content);
        listener.onProgress(Messages.WRITE_GENERATE, 1, 1);
        String text = llm.complete(prompt);
        if (text == null || text.isBlank()) {
            throw new TaskException(Messages.WRITE_EMPTY);
        }
        validateCitations(text, context.size());
        return new Answer(instruction, text.strip(), context.stream().map(c -> Citation.from(c, 240)).toList());
    }

    private void validateCitations(String text, int count) {
        var matcher = CITATION.matcher(text);
        boolean found = false;
        while (matcher.find()) {
            found = true;
            try {
                int number = Integer.parseInt(matcher.group(1));
                if (number < 1 || number > count) {
                    throw new TaskException(Messages.WRITE_INVALID_CITATION);
                }
            } catch (NumberFormatException ex) {
                throw new TaskException(Messages.WRITE_INVALID_CITATION);
            }
        }
        if (!found) {
            throw new TaskException(Messages.WRITE_INVALID_CITATION);
        }
    }
}
