package dev.abyssace.kumo.summarize;

import dev.abyssace.kumo.chunk.Chunker;
import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.i18n.Messages;

/** Patrón: Strategy concreta, una petición con todo el documento si cabe. */
public final class StuffSummarizer implements Summarizer {
    @Override
    public String summarize(Document document, Chunker chunker, SummarySession session) {
        session.progress(Messages.SUMMARY_FINAL, 1, 1);
        return session.complete(document.text(), false);
    }
}
