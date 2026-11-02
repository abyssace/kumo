package dev.abyssace.kumo.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModelNamesTest {

    @Test
    void aceptaElNombreSinTagCuandoOllamaDevuelveLatest() {
        assertTrue(ModelNames.isInstalled("embeddinggemma", List.of("embeddinggemma:latest")));
    }

    @Test
    void aceptaElNombreExactoConTag() {
        assertTrue(ModelNames.isInstalled("qwen3:8b", List.of("qwen3:8b")));
        assertTrue(ModelNames.isInstalled("embeddinggemma:latest", List.of("embeddinggemma:latest")));
    }

    @Test
    void ignoraEspaciosAlrededorDelNombreConfigurado() {
        assertTrue(ModelNames.isInstalled("  embeddinggemma  ", List.of("embeddinggemma:latest")));
    }

    @Test
    void noAceptaOtroTagDistintoDeLatest() {
        assertFalse(ModelNames.isInstalled("embeddinggemma", List.of("embeddinggemma:q4")));
    }

    @Test
    void devuelveFalseSiNoEstaInstalado() {
        assertFalse(ModelNames.isInstalled("qwen3:8b", List.of("llama3.1:8b")));
        assertFalse(ModelNames.isInstalled("qwen3:8b", List.of()));
    }

    @Test
    void validaLosArgumentos() {
        assertThrows(NullPointerException.class, () -> ModelNames.isInstalled(null, List.of()));
        assertThrows(NullPointerException.class, () -> ModelNames.isInstalled("modelo", null));
    }
}
