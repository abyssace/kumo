package dev.abyssace.kumo.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.domain.DocumentFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentLoaderFactoryTest {

    @TempDir
    Path tempDir;

    private final DocumentLoaderFactory factory = new DocumentLoaderFactory();

    @Test
    void eligeElLoaderSegunLaExtension() throws IOException {
        Path txt = tempDir.resolve("a.txt");
        Files.writeString(txt, "texto plano", StandardCharsets.UTF_8);
        Path md = tempDir.resolve("b.md");
        Files.writeString(md, "# titulo", StandardCharsets.UTF_8);
        Path pdf = PdfFixtures.create(tempDir, "c.pdf", List.of("pagina"));

        assertEquals(DocumentFormat.TXT, factory.load(txt).format());
        assertEquals(DocumentFormat.MARKDOWN, factory.load(md).format());
        assertEquals(DocumentFormat.PDF, factory.load(pdf).format());
    }

    @Test
    void exponeLasExtensionesSoportadasOrdenadas() {
        assertEquals(Set.of("md", "markdown", "pdf", "txt"), factory.supportedExtensions());
        assertTrue(factory.supports(Path.of("a.PDF")));
        assertFalse(factory.supports(Path.of("a.docx")));
    }

    @Test
    void formatoNoSoportadoFallaConMensajeUtil() {
        DocumentLoadException error = assertThrows(
                DocumentLoadException.class,
                () -> factory.load(tempDir.resolve("a.docx")));

        assertTrue(error.getMessage().contains("no soportado"));
        assertTrue(error.getMessage().contains("pdf"));
    }

    @Test
    void exigeAlMenosUnLoader() {
        assertThrows(IllegalArgumentException.class, () -> new DocumentLoaderFactory(List.of()));
    }
}
