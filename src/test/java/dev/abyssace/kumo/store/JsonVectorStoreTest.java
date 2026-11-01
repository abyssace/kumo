package dev.abyssace.kumo.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.EmbeddedChunk;
import dev.abyssace.kumo.domain.ScoredChunk;
import dev.abyssace.kumo.domain.SourceLocation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonVectorStoreTest {

    private static final String MODEL = "embeddinggemma";

    @TempDir
    Path tempDir;

    @Test
    void guardaYRecuperaElIndice() {
        JsonVectorStore store = open(MODEL);
        store.add(List.of(
                chunk("docs/a.txt", 0, "gato", new SourceLocation.LineRange(1, 2), 1, 0),
                chunk("docs/b.pdf", 1, "perro", new SourceLocation.PageRef(3), 0, 1)));

        assertTrue(Files.isRegularFile(index()));

        JsonVectorStore reopened = open(MODEL);

        assertEquals(2, reopened.size());
        assertEquals("gato", reopened.search(new float[] {1, 0}, 1).get(0).embedded().chunk().text());
        assertEquals(
                new SourceLocation.PageRef(3),
                reopened.search(new float[] {0, 1}, 1).get(0).embedded().chunk().location());
    }

    @Test
    void elJsonEsLegibleYDeclaraLosMetadatos() throws IOException {
        open(MODEL).add(List.of(
                chunk("docs/a.txt", 0, "gato", new SourceLocation.LineRange(1, 2), 1, 0)));

        String json = Files.readString(index(), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"formatVersion\""));
        assertTrue(json.contains("\"embeddingModel\""));
        assertTrue(json.contains(MODEL));
        assertTrue(json.contains("\"dimensions\""));
        assertTrue(json.contains("\"gato\""));
        assertTrue(json.contains("\"type\""));
        assertTrue(json.contains("\"lines\""));
        assertTrue(json.lines().count() > 5, "El índice debería estar indentado y ser legible");
    }

    @Test
    void conservaLasUbicaciones() {
        open(MODEL).add(List.of(
                chunk("a.txt", 0, "uno", new SourceLocation.LineRange(4, 9), 1, 0),
                chunk("b.pdf", 0, "dos", new SourceLocation.PageRef(7), 0, 1),
                chunk("c.txt", 0, "tres", SourceLocation.none(), 0.5f, 0.5f)));

        Map<String, SourceLocation> locations = open(MODEL).search(new float[] {1, 1}, 3).stream()
                .collect(Collectors.toMap(
                        scored -> scored.embedded().chunk().text(),
                        scored -> scored.embedded().chunk().location()));

        assertEquals(new SourceLocation.LineRange(4, 9), locations.get("uno"));
        assertEquals(new SourceLocation.PageRef(7), locations.get("dos"));
        assertEquals(SourceLocation.none(), locations.get("tres"));
    }

    @Test
    void conservaElTextoUtf8() {
        String texto = "café, ñandú, 日本語 🐈";
        open(MODEL).add(List.of(
                chunk("docs/ñ.txt", 0, texto, SourceLocation.none(), 1, 0)));

        assertEquals(
                texto,
                open(MODEL).search(new float[] {1, 0}, 1).get(0).embedded().chunk().text());
    }

    @Test
    void rechazaOtroModeloDeEmbeddingsConInstruccionDeReindexar() {
        open(MODEL).add(List.of(chunk("a.txt", 0, "gato", SourceLocation.none(), 1, 0)));

        IndexModelMismatchException error = assertThrows(
                IndexModelMismatchException.class, () -> open("bge-m3"));

        assertTrue(error.getMessage().contains(MODEL));
        assertTrue(error.getMessage().contains("bge-m3"));
        assertTrue(error.getMessage().toLowerCase().contains("reindexa"));
    }

    @Test
    void rechazaDimensionesIncompatiblesAlAnadirSinRomperElIndice() {
        JsonVectorStore store = open(MODEL);
        store.add(List.of(chunk("a.txt", 0, "gato", SourceLocation.none(), 1, 0)));

        assertThrows(
                IllegalArgumentException.class,
                () -> store.add(List.of(chunk("b.txt", 0, "perro", SourceLocation.none(), 1, 0, 0))));

        assertEquals(1, store.size());
        assertEquals(1, open(MODEL).size());
    }

    @Test
    void rechazaJsonCorrupto() throws IOException {
        Files.createDirectories(index().getParent());
        Files.writeString(index(), "{ esto no es json", StandardCharsets.UTF_8);

        IndexFormatException error = assertThrows(IndexFormatException.class, () -> open(MODEL));

        assertTrue(error.getMessage().contains("No se pudo leer"));
    }

    @Test
    void rechazaUnaVersionDeFormatoDesconocida() throws IOException {
        writeRaw("""
                {"formatVersion": 99, "embeddingModel": "embeddinggemma", "dimensions": 2, "chunks": []}
                """);

        IndexFormatException error = assertThrows(IndexFormatException.class, () -> open(MODEL));

        assertTrue(error.getMessage().contains("99"));
    }

    @Test
    void rechazaDimensionesIncoherentesEnElArchivo() throws IOException {
        writeRaw("""
                {
                  "formatVersion": 1,
                  "embeddingModel": "embeddinggemma",
                  "dimensions": 3,
                  "chunks": [
                    {
                      "id": "x",
                      "source": "a.txt",
                      "index": 0,
                      "text": "uno",
                      "location": {"type": "none"},
                      "vector": [1.0, 0.0]
                    }
                  ]
                }
                """);

        IndexFormatException error = assertThrows(IndexFormatException.class, () -> open(MODEL));

        assertTrue(error.getMessage().contains("dimensiones"));
    }

    @Test
    void clearBorraElArchivoYDejaElStoreVacio() {
        JsonVectorStore store = open(MODEL);
        store.add(List.of(chunk("a.txt", 0, "gato", SourceLocation.none(), 1, 0)));

        store.clear();

        assertEquals(0, store.size());
        assertFalse(Files.exists(index()));
        assertTrue(store.search(new float[] {1, 0}, 5).isEmpty());
        assertEquals(0, open(MODEL).size());
    }

    @Test
    void noCreaArchivoHastaQueHayFragmentos() {
        JsonVectorStore store = open(MODEL);

        assertEquals(0, store.size());
        assertFalse(Files.exists(index()));
    }

    @Test
    void indexPathApuntaADotKumo() {
        assertEquals(
                tempDir.toAbsolutePath().normalize().resolve(".kumo").resolve("index.json"),
                JsonVectorStore.indexPath(tempDir));
    }

    @Test
    void validaLosArgumentos() {
        assertThrows(NullPointerException.class, () -> new JsonVectorStore(null, new CosineSimilarity(), MODEL));
        assertThrows(
                IllegalArgumentException.class,
                () -> new JsonVectorStore(index(), new CosineSimilarity(), "  "));
    }

    private JsonVectorStore open(String model) {
        return new JsonVectorStore(index(), new CosineSimilarity(), model);
    }

    private Path index() {
        return JsonVectorStore.indexPath(tempDir);
    }

    private void writeRaw(String json) throws IOException {
        Files.createDirectories(index().getParent());
        Files.writeString(index(), json, StandardCharsets.UTF_8);
    }

    private static EmbeddedChunk chunk(
            String source, int index, String text, SourceLocation location, float... vector) {
        return new EmbeddedChunk(Chunk.of(Path.of(source), index, text, location), vector);
    }
}
