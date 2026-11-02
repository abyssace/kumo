package dev.abyssace.kumo.render;

import dev.abyssace.kumo.domain.Answer;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.summarize.SummaryLanguage;
import java.util.Locale;

/** Patrón: View; lista determinista de fuentes recuperadas, no fuentes inventadas por el modelo. */
public final class WriteMarkdownRenderer {
    public String render(Answer answer, SummaryLanguage language, boolean verbose, int topK) {
        boolean english = language == SummaryLanguage.ENGLISH;
        StringBuilder result = new StringBuilder(english ? Messages.WRITE_NOTICE_EN
                : "> " + Messages.WRITE_COVERAGE.formatted(topK));
        result.append("\n\n").append(answer.text()).append("\n\n")
                .append(english ? Messages.WRITE_SOURCES_EN : Messages.WRITE_SOURCES).append("\n\n");
        int number = 1;
        for (var citation : answer.citations()) {
            result.append('[').append(number++).append("] ")
                    .append(escape(citation.source().toString())).append(" | ")
                    .append(escape(citation.location().describe()));
            if (verbose) {
                result.append(String.format(Locale.ROOT, Messages.SCORE_SUFFIX, citation.score()));
            }
            result.append("\n\n> ").append(escape(citation.excerpt())).append("\n\n");
        }
        return result.toString().stripTrailing();
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]")
                .replace("*", "\\*").replace("_", "\\_").replace("`", "\\`");
    }
}
