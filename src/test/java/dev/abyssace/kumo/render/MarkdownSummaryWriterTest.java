package dev.abyssace.kumo.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.abyssace.kumo.summarize.SummaryException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MarkdownSummaryWriterTest {
    @TempDir Path directory;

    @Test
    void guardaUtf8YNoDejaTemporales() throws Exception {
        Path output = directory.resolve("resumen.md");
        new MarkdownSummaryWriter().write(output, "## Conclusión\nInformación útil.");
        assertEquals("## Conclusión\nInformación útil." + System.lineSeparator(), Files.readString(output));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void revalidaElDestinoDespuesDeGenerar() throws Exception {
        var writer = new MarkdownSummaryWriter();
        Path output = directory.resolve("resumen.md");
        writer.validate(output);
        Files.writeString(output, "trabajo del usuario");
        assertThrows(SummaryException.class, () -> writer.write(output, "nuevo"));
        assertEquals("trabajo del usuario", Files.readString(output));
    }

    @Test
    void rechazaUnaCarpetaComoSalida() {
        assertThrows(SummaryException.class, () -> new MarkdownSummaryWriter().write(directory, "nuevo"));
    }
}
