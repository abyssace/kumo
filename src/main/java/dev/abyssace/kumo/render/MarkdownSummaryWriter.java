package dev.abyssace.kumo.render;

import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.summarize.SummaryException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Patrón: Adapter de salida. Publica un archivo completo sin reemplazar archivos existentes. */
public final class MarkdownSummaryWriter {
    public void validate(Path output) {
        if (Files.exists(output, LinkOption.NOFOLLOW_LINKS)) {
            throw new SummaryException(Messages.SUMMARY_OUTPUT_EXISTS.formatted(output));
        }
        if (output.getParent() == null || !Files.isDirectory(output.getParent())) {
            throw new SummaryException(Messages.SUMMARY_OUTPUT_ERROR.formatted(output,
                    Messages.SUMMARY_OUTPUT_PARENT));
        }
    }

    public void write(Path output, String markdown) {
        validate(output);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(output.getParent(), ".kumo-summary-", ".tmp");
            Files.writeString(temporary, markdown + System.lineSeparator(), StandardCharsets.UTF_8);
            // Sin REPLACE_EXISTING: también protege si aparece un destino durante la generación.
            Files.move(temporary, output);
        } catch (IOException ex) {
            throw new SummaryException(Messages.SUMMARY_OUTPUT_ERROR.formatted(output, ex.getMessage()));
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // No oculta el error original si el sistema impide limpiar el temporal.
                }
            }
        }
    }
}
