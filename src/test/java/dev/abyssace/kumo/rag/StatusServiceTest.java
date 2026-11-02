package dev.abyssace.kumo.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.config.ConfigException;
import dev.abyssace.kumo.config.ConfigManager;
import dev.abyssace.kumo.config.StoreType;
import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.EmbeddedChunk;
import dev.abyssace.kumo.domain.SourceLocation;
import dev.abyssace.kumo.fake.FakeOllamaClient;
import dev.abyssace.kumo.llm.OllamaException;
import dev.abyssace.kumo.llm.OllamaModel;
import dev.abyssace.kumo.llm.OllamaUnavailableException;
import dev.abyssace.kumo.store.CosineSimilarity;
import dev.abyssace.kumo.store.JsonVectorStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StatusServiceTest {

    @TempDir
    Path tempDir;

    private Path corpus;
    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        corpus = tempDir.resolve("corpus").toAbsolutePath().normalize();
        configManager = new ConfigManager(tempDir.resolve("home"));
        configManager.save(AppConfig.builder()
                .chatModel("qwen3:8b")
                .embeddingModel("embeddinggemma")
                .vectorStore(StoreType.JSON)
                .build());
    }

    @Test
    void reportaUnEntornoSinIndiceYOllamaCaido() {
        FakeOllamaClient client = new FakeOllamaClient()
                .failListModels(new OllamaUnavailableException("Ollama no responde"));
        StatusService service = new StatusService(configManager, client);

        StatusReport report = service.report(corpus);

        assertEquals(configManager.configFile(), report.configFile());
        assertEquals(corpus, report.corpus());
        assertFalse(report.ollama().reachable());
        assertTrue(report.ollama().error().isPresent());
        assertTrue(report.ollama().installedModels().isEmpty());
        assertFalse(report.ollama().chatModelInstalled());
        assertFalse(report.ollama().embeddingModelInstalled());
        assertTrue(report.ollama().embeddingContextLength().isEmpty());
        assertFalse(report.index().exists());
        assertFalse(report.index().isCorrupt());
        assertEquals(JsonVectorStore.indexPath(corpus), report.index().indexFile());
    }

    @Test
    void reportaOllamaConectadoConLosDosModelos() {
        FakeOllamaClient client = new FakeOllamaClient()
                .models(List.of(
                        new OllamaModel("qwen3:8b", 1),
                        new OllamaModel("embeddinggemma:latest", 2)))
                .contextLength(2048);
        StatusService service = new StatusService(configManager, client);

        StatusReport report = service.report(corpus);

        assertTrue(report.ollama().reachable());
        assertTrue(report.ollama().chatModelInstalled());
        assertTrue(report.ollama().embeddingModelInstalled());
        assertEquals(2048, report.ollama().embeddingContextLength().getAsInt());
        assertEquals(List.of("qwen3:8b", "embeddinggemma:latest"), report.ollama().installedModels());
        assertTrue(report.ollama().chunkFitsContext(400));
        assertFalse(report.ollama().chunkFitsContext(2048));
        assertEquals("embeddinggemma", client.lastShowModel());
    }

    @Test
    void reportaElModeloDeChatFaltante() {
        FakeOllamaClient client = new FakeOllamaClient()
                .models(List.of(new OllamaModel("embeddinggemma:latest", 1)))
                .contextLength(2048);
        StatusService service = new StatusService(configManager, client);

        StatusReport report = service.report(corpus);

        assertTrue(report.ollama().reachable());
        assertFalse(report.ollama().chatModelInstalled());
        assertTrue(report.ollama().embeddingModelInstalled());
    }

    @Test
    void noPideShowSiFaltaElModeloDeEmbeddings() {
        FakeOllamaClient client = new FakeOllamaClient()
                .models(List.of(new OllamaModel("qwen3:8b", 1)));
        StatusService service = new StatusService(configManager, client);

        StatusReport report = service.report(corpus);

        assertFalse(report.ollama().embeddingModelInstalled());
        assertTrue(report.ollama().embeddingContextLength().isEmpty());
        assertNull(client.lastShowModel());
    }

    @Test
    void unFalloDeShowMantieneOllamaConectadoSinContexto() {
        FakeOllamaClient client = new FakeOllamaClient()
                .models(List.of(new OllamaModel("embeddinggemma:latest", 1)))
                .failShowModel(new OllamaException("show caído"));
        StatusService service = new StatusService(configManager, client);

        StatusReport report = service.report(corpus);

        assertTrue(report.ollama().reachable());
        assertTrue(report.ollama().embeddingModelInstalled());
        assertTrue(report.ollama().embeddingContextLength().isEmpty());
    }

    @Test
    void reportaUnIndiceValido() {
        JsonVectorStore store = new JsonVectorStore(
                JsonVectorStore.indexPath(corpus), new CosineSimilarity(), "embeddinggemma");
        store.add(List.of(new EmbeddedChunk(
                Chunk.of(Path.of("a.txt"), 0, "texto", SourceLocation.none()),
                new float[] {1.0f, 0.0f})));
        FakeOllamaClient client = new FakeOllamaClient()
                .models(List.of(new OllamaModel("embeddinggemma:latest", 1)));
        StatusService service = new StatusService(configManager, client);

        StatusReport report = service.report(corpus);

        assertTrue(report.index().exists());
        assertFalse(report.index().isCorrupt());
        assertTrue(report.index().metadata().isPresent());
        assertEquals("embeddinggemma", report.index().metadata().get().embeddingModel());
        assertEquals(2, report.index().metadata().get().dimensions());
        assertEquals(1, report.index().metadata().get().chunks());
    }

    @Test
    void reportaUnIndiceDeOtroModeloSinFallar() {
        JsonVectorStore store = new JsonVectorStore(
                JsonVectorStore.indexPath(corpus), new CosineSimilarity(), "bge-m3");
        store.add(List.of(new EmbeddedChunk(
                Chunk.of(Path.of("a.txt"), 0, "texto", SourceLocation.none()),
                new float[] {1.0f, 0.0f})));
        StatusService service = new StatusService(configManager, new FakeOllamaClient());

        StatusReport report = service.report(corpus);

        assertTrue(report.index().exists());
        assertEquals("bge-m3", report.index().metadata().get().embeddingModel());
    }

    @Test
    void reportaUnIndiceCorrupto() throws IOException {
        Path index = JsonVectorStore.indexPath(corpus);
        Files.createDirectories(index.getParent());
        Files.writeString(index, "{ roto", StandardCharsets.UTF_8);
        StatusService service = new StatusService(configManager, new FakeOllamaClient());

        StatusReport report = service.report(corpus);

        assertFalse(report.index().exists());
        assertTrue(report.index().isCorrupt());
        assertTrue(report.index().error().isPresent());
    }

    @Test
    void propagaLosErroresDeConfiguracion() throws IOException {
        Files.createDirectories(configManager.home());
        Files.writeString(configManager.configFile(), "{ roto", StandardCharsets.UTF_8);
        StatusService service = new StatusService(configManager, new FakeOllamaClient());

        assertThrows(ConfigException.class, () -> service.report(corpus));
    }
}
