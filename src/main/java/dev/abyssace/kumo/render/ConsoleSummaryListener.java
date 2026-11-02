package dev.abyssace.kumo.render;

import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.summarize.SummaryListener;
import dev.abyssace.kumo.summarize.SummaryStrategy;
import java.io.PrintWriter;
import java.util.Objects;

/** Patrón: Observer concreto; solo escribe progreso, nunca el Markdown final. */
public final class ConsoleSummaryListener implements SummaryListener {
    private final PrintWriter err;

    public ConsoleSummaryListener(PrintWriter err) {
        this.err = Objects.requireNonNull(err, "err");
    }

    @Override
    public void onStrategy(SummaryStrategy strategy, String reason, int tokens, int budget) {
        err.println(Messages.SUMMARY_CHOICE.formatted(strategy, reason, tokens, budget));
        err.flush();
    }

    @Override
    public void onProgress(String stage, int current, int total) {
        err.println(Messages.SUMMARY_STAGE.formatted(stage, current, total));
        err.flush();
    }
}
