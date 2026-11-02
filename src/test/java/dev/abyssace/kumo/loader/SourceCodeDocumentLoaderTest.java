package dev.abyssace.kumo.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceCodeDocumentLoaderTest {
    @TempDir Path directory;

    @Test
    void admiteLasExtensionesAcordadasYConservaElTexto() throws Exception {
        var factory = DocumentLoaderFactory.forSummaries();
        String code = "    print('¡hola!')\n";
        for (String extension : List.of("java", "py", "js", "ts", "c", "cpp", "h", "cs",
                "go", "rs", "html", "css", "sql")) {
            Path file = directory.resolve("sample." + extension.toUpperCase());
            Files.writeString(file, code);
            assertEquals(code, factory.load(file).text());
        }
    }

    @Test
    void noEjecutaElCodigoNiAceptaBinarios() throws Exception {
        Path marker = directory.resolve("no-debe-existir");
        Path source = directory.resolve("script.py");
        Files.writeString(source, "open(r'" + marker + "', 'w').write('ejecutado')");
        DocumentLoaderFactory.forSummaries().load(source);
        assertFalse(Files.exists(marker));
        assertFalse(DocumentLoaderFactory.forSummaries().supports(Path.of("program.exe")));
        assertFalse(new DocumentLoaderFactory().supports(source));
    }

    @Test
    void rechazaCodigoVacioOCodificacionInvalida() throws Exception {
        Path source = directory.resolve("main.java");
        Files.writeString(source, "  ");
        assertThrows(DocumentLoadException.class, () -> DocumentLoaderFactory.forSummaries().load(source));
        Files.write(source, new byte[] {(byte) 0xc3, 0x28});
        assertThrows(DocumentLoadException.class, () -> DocumentLoaderFactory.forSummaries().load(source));
    }
}
