package dev.abyssace.kumo.summarize;

import dev.abyssace.kumo.chunk.TokenEstimator;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.llm.LlmClient;
import dev.abyssace.kumo.prompt.SummaryPromptBuilder;
import java.util.Objects;

/** Patrón: inyección por constructor + Guard compartido por las estrategias. */
public final class SummarySession {
    private final LlmClient llm;
    private final TokenEstimator estimator;
    private final SummaryPromptBuilder prompts;
    private final SummaryListener listener;
    private final int numCtx;
    private final int reserve;
    private final int budget;

    public SummarySession(LlmClient llm, TokenEstimator estimator, SummaryPromptBuilder prompts,
            SummaryListener listener, int numCtx, double ratio) {
        this.llm = Objects.requireNonNull(llm, "llm");
        this.estimator = Objects.requireNonNull(estimator, "estimator");
        this.prompts = Objects.requireNonNull(prompts, "prompts");
        this.listener = Objects.requireNonNull(listener, "listener");
        this.numCtx = numCtx;
        // Reserva estimada para respuesta y 64 tokens adicionales de plantilla de chat.
        reserve = Math.max(256, numCtx / 5) + 64;
        int overhead = Math.max(estimator.estimate(prompts.build("", true)),
                estimator.estimate(prompts.build("", false))) + 2;
        budget = Math.min((int) Math.floor(numCtx * ratio), numCtx - overhead - reserve);
        if (budget < 64) {
            throw new SummaryException(Messages.SUMMARY_SMALL_CONTEXT);
        }
    }

    public int budget() {
        return budget;
    }

    public int tokens(String text) {
        return estimator.estimate(text);
    }

    public void progress(String stage, int current, int total) {
        listener.onProgress(stage, current, total);
    }

    public String complete(String text, boolean partial) {
        String prompt = prompts.build(text, partial);
        if (tokens(text) > budget || tokens(prompt) + reserve > numCtx) {
            throw new SummaryException(Messages.SUMMARY_TOO_LARGE.formatted(budget));
        }
        String result = llm.complete(prompt);
        if (result == null || result.isBlank()) {
            throw new SummaryException(Messages.SUMMARY_EMPTY);
        }
        return result.strip();
    }
}
