package dev.abyssace.kumo.summarize;

import dev.abyssace.kumo.chunk.SummaryChunker;
import dev.abyssace.kumo.chunk.TokenEstimator;
import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.llm.LlmClient;
import dev.abyssace.kumo.prompt.SummaryPromptBuilder;
import java.util.Objects;

/** Patrón: Facade + selección de Strategy + inyección por constructor. No usa índice ni embeddings. */
public final class SummaryService {
    private final LlmClient llm;
    private final TokenEstimator estimator;
    private final AppConfig config;

    public SummaryService(LlmClient llm, TokenEstimator estimator, AppConfig config) {
        this.llm = Objects.requireNonNull(llm, "llm");
        this.estimator = Objects.requireNonNull(estimator, "estimator");
        this.config = Objects.requireNonNull(config, "config");
    }

    public String summarize(Document document, SummaryStrategy requested, SummaryLanguage language,
            boolean sourceCode, SummaryListener listener) {
        return summarize(document, requested, language, sourceCode, listener, "");
    }

    /** Variante con foco de extracción, usada por brief sin alterar el resumen normal. */
    public String summarize(Document document, SummaryStrategy requested, SummaryLanguage language,
            boolean sourceCode, SummaryListener listener, String focus) {
        Objects.requireNonNull(requested, "requested");
        SummarySession session = new SummarySession(llm, estimator,
                new SummaryPromptBuilder(language, sourceCode, focus), listener,
                config.numCtx(), config.summaryInputRatio());
        int tokens = session.tokens(document.text());
        boolean fits = tokens <= session.budget();
        SummaryStrategy chosen = requested == SummaryStrategy.AUTO
                ? (fits ? SummaryStrategy.STUFF : SummaryStrategy.MAP_REDUCE) : requested;
        String reason = requested == SummaryStrategy.AUTO
                ? (fits ? Messages.SUMMARY_FITS : Messages.SUMMARY_EXCEEDS) : Messages.SUMMARY_FORCED;
        listener.onStrategy(chosen, reason, tokens, session.budget());
        if (chosen == SummaryStrategy.STUFF && !fits) {
            throw new SummaryException(Messages.SUMMARY_TOO_LARGE.formatted(session.budget()));
        }
        Summarizer strategy = chosen == SummaryStrategy.STUFF
                ? new StuffSummarizer() : new MapReduceSummarizer(estimator);
        return strategy.summarize(document, new SummaryChunker(estimator, session.budget()), session);
    }
}
