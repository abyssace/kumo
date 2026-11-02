package dev.abyssace.kumo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void loadOrCreateEscribeDefaultsConIndiceJson() throws IOException {
        ConfigManager manager = new ConfigManager(tempDir.resolve("home"));

        assertFalse(manager.exists());
        AppConfig config = manager.loadOrCreate();

        assertTrue(manager.exists());
        assertEquals(StoreType.JSON, config.vectorStore());
        assertEquals(AppConfig.DEFAULT_CHAT_MODEL, config.chatModel());
        assertEquals(AppConfig.DEFAULT_EMBEDDING_MODEL, config.embeddingModel());

        String json = Files.readString(manager.configFile(), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"vectorStore\""));
        assertTrue(json.contains("\"JSON\""));
        assertTrue(json.contains(AppConfig.DEFAULT_EMBEDDING_MODEL));
        assertTrue(json.lines().count() > 5, "La configuración debe ser legible");
    }

    @Test
    void saveYLoadHacenRoundTrip() {
        ConfigManager manager = new ConfigManager(tempDir.resolve("home"));
        AppConfig custom = AppConfig.builder()
                .ollamaBaseUrl("http://127.0.0.1:1234")
                .chatModel("llama3.2:3b")
                .embeddingModel("bge-m3")
                .numCtx(4096)
                .embeddingContextTokens(1024)
                .chunkSizeTokens(200)
                .chunkOverlapTokens(40)
                .topK(3)
                .minScore(0.2)
                .verbose(true)
                .vectorStore(StoreType.JSON)
                .build();

        manager.save(custom);

        assertEquals(custom, manager.load());
        assertFalse(Files.exists(manager.configFile().resolveSibling("config.json.tmp")));
    }

    @Test
    void configuracionParcialHeredaLosDefaults() throws IOException {
        ConfigManager manager = new ConfigManager(tempDir.resolve("home"));
        Files.createDirectories(manager.home());
        Files.writeString(
                manager.configFile(),
                "{\"chatModel\":\"llama3.2:3b\"}",
                StandardCharsets.UTF_8);

        AppConfig config = manager.load();

        assertEquals("llama3.2:3b", config.chatModel());
        assertEquals(AppConfig.DEFAULT_EMBEDDING_MODEL, config.embeddingModel());
        assertEquals(AppConfig.DEFAULT_NUM_CTX, config.numCtx());
        assertEquals(StoreType.JSON, config.vectorStore());
    }

    @Test
    void loadSinArchivoDevuelveDefaultsSinEscribir() {
        ConfigManager manager = new ConfigManager(tempDir.resolve("home"));

        AppConfig config = manager.load();

        assertEquals(StoreType.JSON, config.vectorStore());
        assertFalse(manager.exists());
    }

    @Test
    void configuracionMalformadaLanzaConfigExceptionConRuta() throws IOException {
        ConfigManager manager = new ConfigManager(tempDir.resolve("home"));
        Files.createDirectories(manager.home());
        Files.writeString(manager.configFile(), "{ esto no es json", StandardCharsets.UTF_8);

        ConfigException error = assertThrows(ConfigException.class, manager::load);

        assertTrue(error.getMessage().contains(manager.configFile().toString()));
    }

    @Test
    void configuracionNulaLanzaConfigException() throws IOException {
        ConfigManager manager = new ConfigManager(tempDir.resolve("home"));
        Files.createDirectories(manager.home());
        Files.writeString(manager.configFile(), "null", StandardCharsets.UTF_8);

        ConfigException error = assertThrows(ConfigException.class, manager::load);

        assertTrue(error.getMessage().contains("vacío"));
    }

    @Test
    void defaultHomeUsaLaPropiedadKumoHome() {
        String previous = System.getProperty(ConfigManager.KUMO_HOME_PROPERTY);
        try {
            System.setProperty(ConfigManager.KUMO_HOME_PROPERTY, tempDir.toString());

            assertEquals(
                    tempDir.toAbsolutePath().normalize(),
                    ConfigManager.defaultHome());
        } finally {
            if (previous == null) {
                System.clearProperty(ConfigManager.KUMO_HOME_PROPERTY);
            } else {
                System.setProperty(ConfigManager.KUMO_HOME_PROPERTY, previous);
            }
        }
    }

    @Test
    void resolveHomePrefiereKumoHomeSobreLaCarpetaPersonal() {
        Path override = tempDir.resolve("otro").toAbsolutePath().normalize();

        assertEquals(override, ConfigManager.resolveHome(override.toString(), tempDir.toString()));
    }

    @Test
    void resolveHomeUsaPuntoKumoCuandoNoHayOverride() {
        Path expected = tempDir.resolve(".kumo").toAbsolutePath().normalize();

        assertEquals(expected, ConfigManager.resolveHome(null, tempDir.toString()));
        assertEquals(expected, ConfigManager.resolveHome("   ", tempDir.toString()));
    }

    @Test
    void resolveHomeSinCarpetaPersonalFalla() {
        assertThrows(ConfigException.class, () -> ConfigManager.resolveHome(null, null));
        assertThrows(ConfigException.class, () -> ConfigManager.resolveHome(" ", ""));
    }
}
