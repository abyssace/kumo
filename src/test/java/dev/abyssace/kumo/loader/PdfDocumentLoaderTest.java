package dev.abyssace.kumo.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.domain.DocumentFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfDocumentLoaderTest {

    @TempDir
    Path tempDir;

    private final PdfDocumentLoader loader = new PdfDocumentLoader();

    @Test
    void extraeTextoYConservaLaNumeracionRealDePaginas() throws IOException {
        Path file = PdfFixtures.create(
                tempDir,
                "informe.pdf",
                Arrays.asList("Primera pagina", null, "Tercera pagina"));

        Document document = loader.load(file);

        assertEquals(DocumentFormat.PDF, document.format());
        assertEquals("informe.pdf", document.title());
        assertEquals(2, document.pages().size());
        assertEquals(1, document.pages().get(0).number());
        assertTrue(document.pages().get(0).text().contains("Primera pagina"));
        assertEquals(3, document.pages().get(1).number());
        assertTrue(document.pages().get(1).text().contains("Tercera pagina"));
    }

    @Test
    void rechazaUnPdfSinTextoExtraible() throws IOException {
        Path file = PdfFixtures.create(tempDir, "escaneo.pdf", Collections.singletonList(null));

        DocumentLoadException error = assertThrows(DocumentLoadException.class, () -> loader.load(file));

        assertTrue(error.getMessage().contains("escaneo"));
    }

    @Test
    void rechazaUnArchivoQueNoEsPdf() throws IOException {
        Path file = tempDir.resolve("falso.pdf");
        Files.writeString(file, "esto no es un PDF", StandardCharsets.UTF_8);

        assertThrows(DocumentLoadException.class, () -> loader.load(file));
    }

    @Test
    void soloSoportaPdf() {
        assertTrue(loader.supports(Path.of("a.pdf")));
        assertTrue(loader.supports(Path.of("a.PDF")));
        assertThrows(
                DocumentLoadException.class,
                () -> loader.load(tempDir.resolve("a.txt")));
    }
}
