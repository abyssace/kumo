package dev.abyssace.kumo.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.domain.DocumentFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TextDocumentLoaderTest {

    @TempDir
    Path tempDir;

    private final TextDocumentLoader loader = new TextDocumentLoader();

    @Test
    void cargaUtf8YQuitaElBom() throws IOException {
        Path file = tempDir.resolve("notas.txt");
        Files.writeString(file, "\uFEFFHola, ¿cómo estás?\nSegunda línea.", StandardCharsets.UTF_8);

        Document document = loader.load(file);

        assertEquals(DocumentFormat.TXT, document.format());
        assertEquals("notas.txt", document.title());
        assertEquals(file.toAbsolutePath().normalize(), document.path());
        assertEquals(1, document.pages().size());
        assertEquals(1, document.pages().get(0).number());
        assertEquals("Hola, ¿cómo estás?\nSegunda línea.", document.pages().get(0).text());
    }

    @Test
    void soportaLaExtensionSinImportarMayusculas() {
        assertTrue(loader.supports(Path.of("a.TXT")));
        assertTrue(loader.supports(Path.of("a.txt")));
        assertFalse(loader.supports(Path.of("a.md")));
        assertFalse(loader.supports(Path.of("sin-extension")));
        assertFalse(loader.supports(Path.of("termina-en-punto.")));
    }

    @Test
    void rechazaArchivoVacio() throws IOException {
        Path file = tempDir.resolve("vacio.txt");
        Files.writeString(file, "   \n\n", StandardCharsets.UTF_8);

        DocumentLoadException error = assertThrows(DocumentLoadException.class, () -> loader.load(file));

        assertTrue(error.getMessage().contains("vacío"));
    }

    @Test
    void rechazaArchivoInexistente() {
        assertThrows(DocumentLoadException.class, () -> loader.load(tempDir.resolve("no-existe.txt")));
    }

    @Test
    void rechazaExtensionAjena() throws IOException {
        Path file = tempDir.resolve("a.md");
        Files.writeString(file, "texto", StandardCharsets.UTF_8);

        assertThrows(DocumentLoadException.class, () -> loader.load(file));
    }
}
