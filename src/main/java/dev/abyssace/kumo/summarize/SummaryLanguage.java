package dev.abyssace.kumo.summarize;

import dev.abyssace.kumo.i18n.Messages;
import java.text.Normalizer;
import java.util.Locale;

/** Patrón: Value Object. Normaliza alias de idioma sin depender de la configuración regional. */
public enum SummaryLanguage {
    DOCUMENT, SPANISH, ENGLISH;

    public static SummaryLanguage parse(String value) {
        String normalized = Normalizer.normalize(value.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "es", "espanol", "spanish" -> SPANISH;
            case "en", "ingles", "english" -> ENGLISH;
            default -> throw new IllegalArgumentException(Messages.SUMMARY_BAD_LANGUAGE.formatted(value));
        };
    }
}
