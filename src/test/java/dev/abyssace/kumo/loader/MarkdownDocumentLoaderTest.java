package dev.abyssace.kumo.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.domain.DocumentFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MarkdownDocumentLoaderTest {

    @TempDir
    Path tempDir;

    private final MarkdownDocumentLoader loader = new MarkdownDocumentLoader();

    @Test
    void cargaMarkdownComoTextoPlano() throws IOException {
        Path file = tempDir.resolve("guia.md");
        Files.writeString(file, "# Título\n\n- uno\n- dos", StandardCharsets.UTF_8);

        Document document = loader.load(file);

        assertEquals(DocumentFormat.MARKDOWN, document.format());
        assertEquals("guia.md", document.title());
        assertEquals(1, document.pages().size());
        assertTrue(document.text().contains("# Título"));
        assertTrue(document.text().contains("- dos"));
    }

    @Test
    void soportaLasDosExtensiones() {
        assertTrue(loader.supports(Path.of("a.md")));
        assertTrue(loader.supports(Path.of("a.markdown")));
        assertTrue(loader.supports(Path.of("a.MARKDOWN")));
        assertFalse(loader.supports(Path.of("a.txt")));
    }
}
