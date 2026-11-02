package dev.abyssace.kumo.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.abyssace.kumo.task.TaskException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentDiscoveryTest {
    @TempDir Path directory;
    private final DocumentLoaderFactory loaders = DocumentLoaderFactory.forSummaries();

    @Test
    void descubreRecursivamenteConOrdenEstableEIgnoraBuildsEIndices() throws Exception {
        Path java = directory.resolve("a.java");
        Files.writeString(java, "class Test {}");
        Path text = Files.createDirectories(directory.resolve("docs")).resolve("b.txt");
        Files.writeString(text, "texto");
        Files.writeString(directory.resolve("image.png"), "binario");
        for (String name : List.of(".kumo", ".git", "target", "build", "dist", "node_modules")) {
            Files.writeString(Files.createDirectories(directory.resolve(name)).resolve("ignorado.txt"), "ignorado");
        }
        assertEquals(List.of(java, text), new DocumentDiscovery().discover(directory, loaders));
    }

    @Test
    void aceptaUnArchivoExplicitoYRechazaAusentesOIncompatibles() throws Exception {
        Path text = directory.resolve("input.md");
        Files.writeString(text, "texto");
        var discovery = new DocumentDiscovery();
        assertEquals(List.of(text), discovery.discover(text, loaders));
        assertThrows(TaskException.class, () -> discovery.discover(directory.resolve("no-existe.txt"), loaders));
        Path binary = directory.resolve("data.bin");
        Files.writeString(binary, "binario");
        assertThrows(DocumentLoadException.class, () -> discovery.discover(binary, loaders));
    }

    @Test
    void informaCuandoNoHayDocumentosCompatibles() {
        assertThrows(TaskException.class, () -> new DocumentDiscovery().discover(directory, loaders));
    }
}
