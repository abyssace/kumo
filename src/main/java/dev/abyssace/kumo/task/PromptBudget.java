package dev.abyssace.kumo.task;

import dev.abyssace.kumo.chunk.TokenEstimator;
import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.i18n.Messages;
import java.util.Objects;

/** Patrón: Guard de presupuesto para prompts completos, esquema incluido. */
public final class PromptBudget {
    private final TokenEstimator estimator;
    private final AppConfig config;

    public PromptBudget(TokenEstimator estimator, AppConfig config) {
        this.estimator = Objects.requireNonNull(estimator, "estimator");
        this.config = Objects.requireNonNull(config, "config");
    }

    public boolean fits(String prompt, String schema, String content) {
        long reserve = Math.max(256, config.numCtx() / 5) + 64L;
        return estimator.estimate(content) <= Math.floor(config.numCtx() * config.summaryInputRatio())
                && (long) estimator.estimate(prompt) + estimator.estimate(schema) + reserve <= config.numCtx();
    }

    public void require(String prompt, String schema, String content) {
        if (!fits(prompt, schema, content)) {
            throw new TaskException(Messages.TASK_CONTEXT_ERROR);
        }
    }
}
