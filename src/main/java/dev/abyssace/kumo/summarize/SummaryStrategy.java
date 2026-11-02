package dev.abyssace.kumo.summarize;

import dev.abyssace.kumo.i18n.Messages;
import java.util.Locale;

/** Patrón: Value Object para la selección de Strategy. */
public enum SummaryStrategy {
    AUTO("auto"), STUFF("stuff"), MAP_REDUCE("map-reduce");

    private final String label;

    SummaryStrategy(String label) {
        this.label = label;
    }

    public static SummaryStrategy parse(String value) {
        for (SummaryStrategy strategy : values()) {
            if (strategy.label.equals(value.toLowerCase(Locale.ROOT).strip())) {
                return strategy;
            }
        }
        throw new IllegalArgumentException(Messages.SUMMARY_BAD_STRATEGY.formatted(value));
    }

    @Override
    public String toString() {
        return label;
    }
}
