package dev.abyssace.kumo.summarize;

/** Patrón: Observer del progreso, independiente de la consola y del modelo. */
public interface SummaryListener {
    default void onStrategy(SummaryStrategy strategy, String reason, int tokens, int budget) { }

    default void onProgress(String stage, int current, int total) { }
}
