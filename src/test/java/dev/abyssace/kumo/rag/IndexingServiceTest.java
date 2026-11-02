package dev.abyssace.kumo.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.chunk.FixedSizeChunker;
import dev.abyssace.kumo.chunk.HeuristicTokenEstimator;
import dev.abyssace.kumo.domain.Document;
import dev.abyssace.kumo.domain.DocumentFormat;
import dev.abyssace.kumo.domain.DocumentPage;
import dev.abyssace.kumo.embed.Embedder;
import dev.abyssace.kumo.fake.FakeEmbedder;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.llm.OllamaException;
import dev.abyssace.kumo.llm.OllamaUnavailableException;
import dev.abyssace.kumo.loader.DocumentLoadException;
import dev.abyssace.kumo.loader.DocumentLoader;
import dev.abyssace.kumo.loader.DocumentLoaderFactory;
import dev.abyssace.kumo.store.CosineSimilarity;
import dev.abyssace.kumo.store.InMemoryVectorStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IndexingServiceTest {

    @TempDir
    Path tempDir;

    private FakeEmbedder embedder;
    private InMemoryVectorStore store;
    private IndexingService service;

    @BeforeEach
    void setUp() {
        embedder = new FakeEmbedder();
        store = new InMemoryVectorStore(new CosineSimilarity());
        service = new IndexingService(
                new DocumentLoaderFactory(),
                new FixedSizeChunker(new HeuristicTokenEstimator(), 20, 5),
                new ChunkIndexer(embedder, store));
    }

    @Test
    void indexaUnaCarpetaYReemplazaAlReindexar() throws IOException {
        Path corpus = Files.createDirectories(tempDir.resolve("corpus"));
        Path file = corpus.resolve("a.txt");
        Files.writeString(file, "alpha beta gamma delta", StandardCharsets.UTF_8);
        RecordingIndexingListener firstListener = new RecordingIndexingListener();

        IndexingSummary first = service.index(List.of(corpus), firstListener);

        assertEquals(1, first.files());
        assertEquals(1, first.chunks());
        assertEquals(0, first.failures());
        assertEquals(1, store.size());
        assertEquals(1, firstListener.startTotal);
        assertEquals(List.of(file.toAbsolutePath().normalize()), firstListener.indexed);
        assertNotNull(firstListener.finished);

        Files.writeString(file, "epsilon zeta eta theta", StandardCharsets.UTF_8);
        RecordingIndexingListener secondListener = new RecordingIndexingListener();

        IndexingSummary second = service.index(List.of(corpus), secondListener);

        assertEquals(1, second.files());
        assertEquals(1, second.chunks());
        assertEquals(0, second.failures());
        assertEquals(1, store.size(), "Reindexar no debe duplicar los fragmentos");
        assertEquals(1, secondListener.indexed.size());
        assertTrue(store.search(embedder.embed("epsilon"), 5).get(0).embedded().chunk().text()
                .contains("epsilon"));
    }

    @Test
    void unArchivoIlegibleNoAbortaElLote() throws IOException {
        Path good = tempDir.resolve("good.txt");
        Files.writeString(good, "contenido válido", StandardCharsets.UTF_8);
        Path bad = tempDir.resolve("bad.bin");
        Files.writeString(bad, "binario", StandardCharsets.UTF_8);
        RecordingIndexingListener listener = new RecordingIndexingListener();

        IndexingSummary summary = service.index(List.of(good, bad), listener);

        assertEquals(1, summary.files());
        assertEquals(1, summary.failures());
        assertEquals(1, listener.indexed.size());
        assertEquals(1, listener.failures.size());
        assertTrue(listener.failures.get(0).contains("bad.bin"));
    }

    @Test
    void unDocumentoSinTextoIndexableCuentaComoFallo() throws IOException {
        DocumentLoader blankLoader = new DocumentLoader() {
            @Override
            public boolean supports(Path path) {
                return true;
            }

            @Override
            public Document load(Path path) {
                return new Document(
                        path,
                        "vacío",
                        DocumentFormat.TXT,
                        List.of(new DocumentPage(1, "   \n  ")));
            }
        };
        IndexingService blankService = new IndexingService(
                new DocumentLoaderFactory(List.of(blankLoader)),
                new FixedSizeChunker(new HeuristicTokenEstimator(), 20, 5),
                new ChunkIndexer(embedder, store));
        Path file = tempDir.resolve("vacio.txt");
        Files.writeString(file, "texto", StandardCharsets.UTF_8);
        RecordingIndexingListener listener = new RecordingIndexingListener();

        IndexingSummary summary = blankService.index(List.of(file), listener);

        assertEquals(0, summary.files());
        assertEquals(1, summary.failures());
        assertEquals(List.of(Messages.EMPTY_DOCUMENT), listener.failures);
    }

    @Test
    void ignoraLasCarpetasInternas() throws IOException {
        Path corpus = Files.createDirectories(tempDir.resolve("corpus"));
        Path docs = Files.createDirectories(corpus.resolve("docs"));
        Path visible = docs.resolve("visible.txt");
        Files.writeString(visible, "texto visible", StandardCharsets.UTF_8);
        Path internal = Files.createDirectories(corpus.resolve(".kumo"));
        Files.writeString(internal.resolve("oculto.txt"), "texto oculto", StandardCharsets.UTF_8);
        RecordingIndexingListener listener = new RecordingIndexingListener();

        IndexingSummary summary = service.index(List.of(corpus), listener);

        assertEquals(1, summary.files());
        assertEquals(List.of(visible.toAbsolutePath().normalize()), listener.indexed);
    }

    @Test
    void unaRutaInexistenteFalla() {
        RecordingIndexingListener listener = new RecordingIndexingListener();

        assertThrows(
                DocumentLoadException.class,
                () -> service.index(List.of(tempDir.resolve("no-existe.txt")), listener));
    }

    @Test
    void notificaElOrdenDeLosArchivos() throws IOException {
        Path first = tempDir.resolve("a.txt");
        Path second = tempDir.resolve("b.txt");
        Files.writeString(first, "uno", StandardCharsets.UTF_8);
        Files.writeString(second, "dos", StandardCharsets.UTF_8);
        RecordingIndexingListener listener = new RecordingIndexingListener();

        service.index(List.of(first, second), listener);

        assertEquals(List.of(first.toAbsolutePath().normalize(), second.toAbsolutePath().normalize()),
                listener.started);
        assertEquals(List.of(1, 2), listener.startedIndexes);
    }

    @Test
    void unFalloDeOllamaNoDisponibleAbortaElLote() throws IOException {
        Embedder failing = new Embedder() {
            @Override
            public float[] embed(String text) {
                throw new OllamaUnavailableException("sin Ollama");
            }

            @Override
            public List<float[]> embedAll(List<String> texts) {
                throw new OllamaUnavailableException("sin Ollama");
            }
        };
        IndexingService failingService = new IndexingService(
                new DocumentLoaderFactory(),
                new FixedSizeChunker(new HeuristicTokenEstimator(), 20, 5),
                new ChunkIndexer(failing, store));
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "contenido", StandardCharsets.UTF_8);
        RecordingIndexingListener listener = new RecordingIndexingListener();

        assertThrows(
                OllamaUnavailableException.class,
                () -> failingService.index(List.of(file), listener));
        assertNull(listener.finished);
    }

    @Test
    void unErrorDeOllamaNoCriticoCuentaComoFalloDeArchivo() throws IOException {
        Embedder failing = new Embedder() {
            @Override
            public float[] embed(String text) {
                throw new OllamaException("fallo de embeddings");
            }

            @Override
            public List<float[]> embedAll(List<String> texts) {
                throw new OllamaException("fallo de embeddings");
            }
        };
        IndexingService failingService = new IndexingService(
                new DocumentLoaderFactory(),
                new FixedSizeChunker(new HeuristicTokenEstimator(), 20, 5),
                new ChunkIndexer(failing, store));
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "contenido", StandardCharsets.UTF_8);
        RecordingIndexingListener listener = new RecordingIndexingListener();

        IndexingSummary summary = failingService.index(List.of(file), listener);

        assertEquals(0, summary.files());
        assertEquals(1, summary.failures());
        assertFalse(listener.failures.isEmpty());
        assertTrue(listener.failures.get(0).contains("fallo de embeddings"));
    }

    @Test
    void exponeLasExtensionesSoportadas() {
        assertEquals(Set.of("markdown", "md", "pdf", "txt"), service.supportedExtensions());
    }

    private static final class RecordingIndexingListener implements IndexingListener {

        private int startTotal = -1;
        private final List<Path> started = new ArrayList<>();
        private final List<Integer> startedIndexes = new ArrayList<>();
        private final List<Path> indexed = new ArrayList<>();
        private final List<String> failures = new ArrayList<>();
        private IndexingSummary finished;

        @Override
        public void onStart(int totalFiles) {
            startTotal = totalFiles;
        }

        @Override
        public void onFileStarted(Path file, int index) {
            started.add(file);
            startedIndexes.add(index);
        }

        @Override
        public void onFileIndexed(Path file, int chunks) {
            indexed.add(file);
        }

        @Override
        public void onFileFailed(Path file, String message) {
            failures.add(message);
        }

        @Override
        public void onFinished(IndexingSummary summary) {
            finished = summary;
        }
    }
}
