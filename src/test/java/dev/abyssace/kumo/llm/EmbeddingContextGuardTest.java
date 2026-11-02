package dev.abyssace.kumo.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.fake.FakeOllamaClient;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmbeddingContextGuardTest {

    @TempDir
    Path tempDir;

    @Test
    void aceptaUnFragmentoQueCabeEnElContexto() {
        FakeOllamaClient client = new FakeOllamaClient().contextLength(2048);
        AppConfig config = AppConfig.builder().chunkSizeTokens(400).build();

        OllamaModelInfo info = EmbeddingContextGuard.validate(
                config, client, tempDir.resolve("config.json"));

        assertEquals("embeddinggemma", info.name());
        assertEquals(2048, info.contextLength().getAsInt());
        assertTrue(info.supportsEmbedding());
        assertEquals("embeddinggemma", client.lastShowModel());
    }

    @Test
    void rechazaUnFragmentoIgualOMayorQueElContexto() {
        FakeOllamaClient client = new FakeOllamaClient().contextLength(400);
        AppConfig config = AppConfig.builder().chunkSizeTokens(400).build();
        Path configFile = tempDir.resolve("config.json");

        ModelContextException error = assertThrows(
                ModelContextException.class,
                () -> EmbeddingContextGuard.validate(config, client, configFile));

        assertTrue(error.getMessage().contains("400"));
        assertTrue(error.getMessage().contains(configFile.toString()));
        assertTrue(error.getMessage().contains("embeddinggemma"));
    }

    @Test
    void confiaEnLaConfiguracionSiOllamaNoPublicaElContexto() {
        FakeOllamaClient client = new FakeOllamaClient();
        AppConfig config = AppConfig.builder().chunkSizeTokens(400).build();

        OllamaModelInfo info = EmbeddingContextGuard.validate(
                config, client, tempDir.resolve("config.json"));

        assertTrue(info.contextLength().isEmpty());
    }

    @Test
    void propagaLosErroresDeShowModel() {
        FakeOllamaClient client = new FakeOllamaClient()
                .failShowModel(new OllamaException("modelo desconocido"));
        AppConfig config = AppConfig.defaults();

        OllamaException error = assertThrows(
                OllamaException.class,
                () -> EmbeddingContextGuard.validate(config, client, tempDir.resolve("config.json")));

        assertEquals("modelo desconocido", error.getMessage());
    }

    @Test
    void validaLosArgumentos() {
        FakeOllamaClient client = new FakeOllamaClient();
        AppConfig config = AppConfig.defaults();
        Path configFile = tempDir.resolve("config.json");

        assertThrows(NullPointerException.class, () -> EmbeddingContextGuard.validate(null, client, configFile));
        assertThrows(NullPointerException.class, () -> EmbeddingContextGuard.validate(config, null, configFile));
        assertThrows(NullPointerException.class, () -> EmbeddingContextGuard.validate(config, client, null));
    }
}
