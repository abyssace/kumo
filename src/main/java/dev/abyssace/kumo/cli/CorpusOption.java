package dev.abyssace.kumo.cli;

import dev.abyssace.kumo.i18n.Messages;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import picocli.CommandLine.Option;

/**
 * Patrón: Mixin de opciones (composición de CLI).
 *
 * <p>La carpeta del corpus se repite en {@code add}, {@code ask} y
 * {@code status}. Este mixin la define una sola vez y ofrece dos formas de
 * resolverla: estricta (debe existir) para indexar y preguntar, y tolerante
 * para {@code status}, que también debe informar de una carpeta vacía.</p>
 */
public final class CorpusOption {

    @Option(
            names = {"-c", "--corpus"},
            paramLabel = "<carpeta>",
            description = Messages.CLI_OPT_CORPUS)
    private Path corpus = Path.of(".");

    /** Ruta absoluta y normalizada, exista o no. */
    public Path path() {
        return Objects.requireNonNull(corpus, "corpus").toAbsolutePath().normalize();
    }

    /** Ruta absoluta que debe ser una carpeta existente. */
    public Path resolve() {
        Path normalized = path();
        if (!Files.isDirectory(normalized)) {
            throw new IllegalArgumentException(Messages.CORPUS_NOT_FOUND.formatted(normalized));
        }
        return normalized;
    }
}
