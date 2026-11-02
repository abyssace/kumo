package dev.abyssace.kumo.render;

import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.rag.IndexingListener;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Patrón: Observer concreto.
 *
 * <p>Muestra el progreso del indexado en la consola. Guarda el índice actual
 * para imprimir una sola línea por archivo cuando ya se conoce el resultado,
 * sin dejar líneas a medias si el archivo falla.</p>
 */
public final class ConsoleIndexingListener implements IndexingListener {

    private final PrintWriter out;
    private final boolean verbose;
    private final Path corpus;
    private int total;
    private int current;

    public ConsoleIndexingListener(PrintWriter out, boolean verbose, Path corpus) {
        this.out = Objects.requireNonNull(out, "out");
        this.verbose = verbose;
        this.corpus = Objects.requireNonNull(corpus, "corpus").toAbsolutePath().normalize();
    }

    @Override
    public void onStart(int totalFiles) {
        this.total = totalFiles;
        if (totalFiles > 0) {
            out.println(Messages.INDEXING_START.formatted(totalFiles, corpus));
        }
    }

    @Override
    public void onFileStarted(Path file, int index) {
        this.current = index;
    }

    @Override
    public void onFileIndexed(Path file, int chunks) {
        out.println(Messages.INDEXING_FILE.formatted(current, total, display(file), chunks));
    }

    @Override
    public void onFileFailed(Path file, String message) {
        out.println(Messages.INDEXING_FAILED.formatted(current, total, display(file), message));
    }

    private String display(Path file) {
        if (verbose) {
            return file.toString();
        }
        try {
            return corpus.relativize(file).toString();
        } catch (IllegalArgumentException ex) {
            Path name = file.getFileName();
            return name == null ? file.toString() : name.toString();
        }
    }
}
