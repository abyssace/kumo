package dev.abyssace.kumo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.store.InMemoryVectorStore;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    void losDefaultsCoincidenConLaEspecificacion() {
        AppConfig config = AppConfig.defaults();

        assertEquals("http://localhost:11434", config.ollamaBaseUrl());
        assertEquals("qwen3:8b", config.chatModel());
        assertEquals("embeddinggemma", config.embeddingModel());
        assertEquals(8192, config.numCtx());
        assertEquals(2048, config.embeddingContextTokens());
        assertEquals(400, config.chunkSizeTokens());
        assertEquals(80, config.chunkOverlapTokens());
        assertEquals(5, config.topK());
        assertEquals(-1.0, config.minScore());
        assertFalse(config.verbose());
        assertEquals(StoreType.MEMORY, config.vectorStore());
    }

    @Test
    void elBuilderSobrescribeLosDefaults() {
        AppConfig config = AppConfig.builder()
                .chatModel("llama3.2:3b")
                .embeddingModel("bge-m3")
                .numCtx(4096)
                .topK(3)
                .minScore(0.2)
                .verbose(true)
                .vectorStore(StoreType.JSON)
                .build();

        assertEquals("llama3.2:3b", config.chatModel());
        assertEquals("bge-m3", config.embeddingModel());
        assertEquals(4096, config.numCtx());
        assertEquals(3, config.topK());
        assertEquals(0.2, config.minScore());
        assertTrue(config.verbose());
        assertEquals(StoreType.JSON, config.vectorStore());
    }

    @Test
    void normalizaLaBarraFinalDeLaUrl() {
        AppConfig config = AppConfig.builder().ollamaBaseUrl("http://localhost:11434/").build();

        assertEquals("http://localhost:11434", config.ollamaBaseUrl());
    }

    @Test
    void rechazaUnaUrlQueNoSeaLocalhost() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> AppConfig.builder().ollamaBaseUrl("http://192.168.1.10:11434").build());

        assertTrue(error.getMessage().contains("localhost"));
    }

    @Test
    void rechazaModeloVacio() {
        assertThrows(IllegalArgumentException.class, () -> AppConfig.builder().chatModel("  ").build());
    }

    @Test
    void rechazaSolapamientoMayorOIgualQueElFragmento() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AppConfig.builder().chunkSizeTokens(100).chunkOverlapTokens(100).build());
    }

    @Test
    void rechazaFragmentoQueNoCabeEnElContextoDelEmbedder() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AppConfig.builder().chunkSizeTokens(2048).embeddingContextTokens(2048).build());
    }

    @Test
    void rechazaTopKNoPositivo() {
        assertThrows(IllegalArgumentException.class, () -> AppConfig.builder().topK(0).build());
    }

    @Test
    void laFabricaCreaElStoreEnMemoria() {
        assertInstanceOf(InMemoryVectorStore.class, StoreFactory.create(AppConfig.defaults()));
    }

    @Test
    void elStoreJsonAunNoEstaDisponible() {
        AppConfig config = AppConfig.builder().vectorStore(StoreType.JSON).build();

        assertThrows(UnsupportedOperationException.class, () -> StoreFactory.create(config));
    }
}
