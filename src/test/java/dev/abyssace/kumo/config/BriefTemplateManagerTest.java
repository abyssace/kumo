package dev.abyssace.kumo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.task.TaskException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BriefTemplateManagerTest {
    @TempDir Path directory;

    @Test
    void creaLaPlantillaGlobalSinReemplazarEdiciones() throws Exception {
        var manager = new BriefTemplateManager(directory.resolve("home"));
        assertEquals(BriefTemplate.defaults(), manager.load(null));
        assertTrue(Files.isRegularFile(manager.defaultPath()));
        String custom = "{\"fields\":[\"title\",\"summary\"],\"instructions\":\"Sé conciso.\"}";
        Files.writeString(manager.defaultPath(), custom);
        assertEquals(List.of("title", "summary"), manager.load(null).fields());
        assertEquals(custom, Files.readString(manager.defaultPath()));
    }

    @Test
    void aceptaUnaPlantillaAlternativaSinCrearLaGlobal() throws Exception {
        Path custom = directory.resolve("custom.json");
        Files.writeString(custom, "{\"fields\":[\"title\",\"summary\"],\"instructions\":\"\"}");
        var manager = new BriefTemplateManager(directory.resolve("home"));
        assertEquals(2, manager.load(custom).fields().size());
        assertFalse(Files.exists(manager.defaultPath()));
    }

    @Test
    void noCreaUnaPlantillaPersonalizadaAusente() {
        var path = directory.resolve("missing.json");
        assertThrows(TaskException.class, () -> new BriefTemplateManager(directory).load(path));
        assertFalse(Files.exists(path));
    }

    @Test
    void rechazaPlantillasInvalidasSinModificarElArchivo() throws Exception {
        Path custom = directory.resolve("custom.json");
        for (String text : List.of("{ roto", "null", "[]", "{}",
                "{\"fields\":[\"title\",\"summary\"],\"instructions\":4}",
                "{\"fields\":[3],\"instructions\":\"\"}",
                "{\"fields\":[\"title\",\"summary\"],\"instructions\":\"\",\"extra\":true}",
                "{\"fields\":[\"title\",\"summary\"],\"instructions\":\"\"} {}")) {
            Files.writeString(custom, text);
            assertThrows(TaskException.class, () -> new BriefTemplateManager(directory).load(custom));
            assertEquals(text, Files.readString(custom));
        }
    }

    @Test
    void validaCamposDuplicadosAusentesDesconocidosEInstrucciones() {
        for (var fields : List.of(List.of("title"), List.of("title", "summary", "summary"),
                List.of("title", "summary", "inventado"))) {
            assertThrows(IllegalArgumentException.class, () -> new BriefTemplate(fields, ""));
        }
        assertThrows(IllegalArgumentException.class,
                () -> new BriefTemplate(List.of("title", "summary"), "x".repeat(4001)));
    }

    @Test
    void writeTopKPersisteYLasConfiguracionesAnterioresSiguenValidas() throws Exception {
        var manager = new ConfigManager(directory);
        manager.save(AppConfig.builder().writeTopK(18).build());
        assertEquals(18, manager.load().writeTopK());
        Files.writeString(manager.configFile(), "{}");
        assertEquals(12, manager.load().writeTopK());
        assertThrows(IllegalArgumentException.class, () -> AppConfig.builder().writeTopK(0).build());
    }
}
