package dev.abyssace.kumo.summarize;

import dev.abyssace.kumo.chunk.BudgetTextSplitter;
import dev.abyssace.kumo.chunk.Chunker;
import dev.abyssace.kumo.chunk.TokenEstimator;
import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.i18n.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Patrón: Strategy concreta; MapReduce secuencial y jerárquico, sin truncado. */
public final class MapReduceSummarizer implements Summarizer {
    private final TokenEstimator estimator;

    public MapReduceSummarizer(TokenEstimator estimator) {
        this.estimator = Objects.requireNonNull(estimator, "estimator");
    }

    @Override
    public String summarize(Document document, Chunker chunker, SummarySession session) {
        var chunks = chunker.split(document);
        session.progress(Messages.SUMMARY_SPLIT, chunks.size(), chunks.size());
        if (chunks.isEmpty()) {
            throw new SummaryException(Messages.EMPTY_DOCUMENT);
        }
        List<String> partials = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            session.progress(Messages.SUMMARY_MAP, i + 1, chunks.size());
            partials.add(session.complete(chunks.get(i).text(), true));
        }
        String combined = String.join("\n\n", partials);
        BudgetTextSplitter splitter = new BudgetTextSplitter(estimator, session.budget());
        int round = 0;
        while (session.tokens(combined) > session.budget()) {
            if (++round > 32) {
                throw new SummaryException(Messages.SUMMARY_NO_CONTRACTION);
            }
            int before = session.tokens(combined);
            List<String> groups = splitter.split(combined);
            List<String> reduced = new ArrayList<>();
            for (int i = 0; i < groups.size(); i++) {
                session.progress(Messages.SUMMARY_REDUCE.formatted(round), i + 1, groups.size());
                reduced.add(session.complete(groups.get(i), true));
            }
            combined = String.join("\n\n", reduced);
            if (session.tokens(combined) >= before) {
                throw new SummaryException(Messages.SUMMARY_NO_CONTRACTION);
            }
        }
        session.progress(Messages.SUMMARY_FINAL, 1, 1);
        return session.complete(combined, false);
    }
}
