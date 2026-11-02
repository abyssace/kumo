package dev.abyssace.kumo.chunk;

import dev.abyssace.kumo.i18n.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Patrón: Strategy auxiliar. Corta incluso tokens léxicos largos sin perder caracteres ni sangría. */
public final class BudgetTextSplitter {
    private final TokenEstimator estimator;
    private final int budget;

    public BudgetTextSplitter(TokenEstimator estimator, int budget) {
        this.estimator = Objects.requireNonNull(estimator, "estimator");
        if (budget < 1) {
            throw new IllegalArgumentException(Messages.SUMMARY_BUDGET_INVALID);
        }
        this.budget = budget;
    }

    public List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int low = start + 1;
            int high = text.length();
            int end = start;
            while (low <= high) {
                int middle = low + (high - low) / 2;
                if (estimator.estimate(text.substring(start, middle)) <= budget) {
                    end = middle;
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }
            if (end < text.length() && end > start && Character.isHighSurrogate(text.charAt(end - 1))) {
                end--;
            }
            if (end == start) {
                throw new IllegalArgumentException(Messages.SUMMARY_SPLIT_ERROR);
            }
            // Prefiere un salto de línea o espacio próximo al final, sin quitarlo.
            if (end < text.length()) {
                for (int i = end - 1; i > start + (end - start) * 3 / 4; i--) {
                    if (Character.isWhitespace(text.charAt(i))) {
                        end = i + 1;
                        break;
                    }
                }
            }
            parts.add(text.substring(start, end));
            start = end;
        }
        return List.copyOf(parts);
    }
}
