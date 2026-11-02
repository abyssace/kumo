package dev.abyssace.kumo.render;

import dev.abyssace.kumo.domain.Citation;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.rag.IndexingSummary;
import dev.abyssace.kumo.rag.StatusReport;
import dev.abyssace.kumo.store.IndexMetadata;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Patrón: View / Renderer.
 *
 * <p>Convierte los resultados de los servicios en texto de consola. No decide
 * nada del dominio: solo formatea citas, estado y resúmenes usando los textos
 * centralizados de {@link Messages}.</p>
 */
public final class ConsoleRenderer {

    private final PrintWriter out;

    public ConsoleRenderer() {
        this(new PrintWriter(System.out, true));
    }

    public ConsoleRenderer(PrintWriter out) {
        this.out = Objects.requireNonNull(out, "out");
    }

    /** Imprime las fuentes de una respuesta. */
    public void printCitations(List<Citation> citations, boolean verbose) {
        Objects.requireNonNull(citations, "citations");
        if (citations.isEmpty()) {
            return;
        }

        out.println();
        out.println(Messages.SOURCES_HEADER);
        int number = 1;
        for (Citation citation : citations) {
            String where = display(citation.source());
            String location = citation.location().describe();
            if (!location.isBlank()) {
                where = where + " | " + location;
            }
            String score = verbose
                    ? String.format(Locale.ROOT, Messages.SCORE_SUFFIX, citation.score())
                    : "";
            out.println(String.format(Locale.ROOT, Messages.SOURCE_LINE, number++, where, score));
            out.println(String.format(Locale.ROOT, Messages.SOURCE_EXCERPT, citation.excerpt()));
        }
    }

    /** Imprime el estado completo del entorno. */
    public void printStatus(StatusReport report) {
        Objects.requireNonNull(report, "report");
        StatusReport.OllamaStatus ollama = report.ollama();
        StatusReport.IndexStatus index = report.index();

        out.println(Messages.STATUS_CONFIG.formatted(report.configFile()));
        out.println(Messages.STATUS_CORPUS.formatted(report.corpus()));
        out.println(Messages.STATUS_INDEX.formatted(indexDescription(index)));
        out.println(Messages.STATUS_OLLAMA.formatted(ollamaDescription(report)));
        out.println(Messages.STATUS_CHAT.formatted(modelDescription(
                report.config().chatModel(), ollama.reachable(), ollama.chatModelInstalled())));
        out.println(Messages.STATUS_EMBEDDING.formatted(embeddingDescription(report)));

        if (ollama.reachable()
                && ollama.embeddingModelInstalled()
                && !ollama.chunkFitsContext(report.config().chunkSizeTokens())) {
            out.println(Messages.OLLAMA_CONTEXT_TOO_SMALL.formatted(
                    report.config().embeddingModel(),
                    ollama.embeddingContextLength().getAsInt(),
                    report.config().chunkSizeTokens(),
                    report.configFile()));
        }
    }

    /** Imprime el resumen de un lote de indexado. */
    public void printIndexingSummary(IndexingSummary summary, Path indexFile) {
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(indexFile, "indexFile");
        if (summary.isEmpty()) {
            return;
        }
        out.println(Messages.INDEXING_SUMMARY.formatted(
                summary.files(), summary.chunks(), summary.failures()));
        if (summary.files() > 0) {
            out.println(Messages.CLI_ADD_SUMMARY.formatted(indexFile));
        }
    }

    /** Imprime un error con el prefijo común. */
    public void printError(String message) {
        out.println(Messages.CLI_ERROR.formatted(message));
    }

    private static String indexDescription(StatusReport.IndexStatus index) {
        if (index.isCorrupt()) {
            return Messages.STATUS_INDEX_CORRUPT.formatted(index.error().orElse(""));
        }
        if (index.metadata().isEmpty()) {
            return Messages.STATUS_INDEX_EMPTY;
        }
        IndexMetadata metadata = index.metadata().get();
        return Messages.STATUS_INDEX_DETAILS.formatted(
                metadata.chunks(), metadata.dimensions(), metadata.embeddingModel());
    }

    private static String ollamaDescription(StatusReport report) {
        if (report.ollama().reachable()) {
            return Messages.STATUS_REACHABLE.formatted(report.config().ollamaBaseUrl());
        }
        return Messages.STATUS_UNREACHABLE.formatted(
                report.ollama().error().orElse("sin detalle"));
    }

    private static String modelDescription(String model, boolean reachable, boolean installed) {
        if (!reachable) {
            return model + " (" + Messages.STATUS_UNKNOWN + ")";
        }
        return model + " (" + (installed ? Messages.STATUS_INSTALLED : Messages.STATUS_MISSING) + ")";
    }

    private static String embeddingDescription(StatusReport report) {
        StatusReport.OllamaStatus ollama = report.ollama();
        String model = report.config().embeddingModel();
        if (!ollama.reachable()) {
            return model + " (" + Messages.STATUS_UNKNOWN + ")";
        }
        if (!ollama.embeddingModelInstalled()) {
            return model + " (" + Messages.STATUS_MISSING + ")";
        }
        if (ollama.embeddingContextLength().isPresent()) {
            return model + " (" + Messages.STATUS_EMBEDDING_CONTEXT.formatted(
                    ollama.embeddingContextLength().getAsInt()) + ")";
        }
        return model + " (" + Messages.STATUS_EMBEDDING_UNKNOWN + ")";
    }

    private static String display(Path source) {
        Path name = source.getFileName();
        return name == null ? source.toString() : name.toString();
    }
}
