package dev.abyssace.kumo.summarize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.config.AppConfig;
import dev.abyssace.kumo.config.ConfigManager;
import dev.abyssace.kumo.prompt.SummaryPromptBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SummaryOptionsTest {
    @TempDir Path home;

    @Test
    void aceptaAliasSinDistinguirTildesNiMayusculas() {
        for (String value : List.of("es", "ESPAÑOL", "espanol", "Spanish")) {
            assertEquals(SummaryLanguage.SPANISH, SummaryLanguage.parse(value));
        }
        for (String value : List.of("en", "INGLÉS", "ingles", "English")) {
            assertEquals(SummaryLanguage.ENGLISH, SummaryLanguage.parse(value));
        }
        assertThrows(IllegalArgumentException.class, () -> SummaryLanguage.parse("fr"));
    }

    @Test
    void validaYNormalizaLasEstrategias() {
        assertEquals(SummaryStrategy.AUTO, SummaryStrategy.parse("AUTO"));
        assertEquals(SummaryStrategy.STUFF, SummaryStrategy.parse("stuff"));
        assertEquals(SummaryStrategy.MAP_REDUCE, SummaryStrategy.parse("map-reduce"));
        assertThrows(IllegalArgumentException.class, () -> SummaryStrategy.parse("otro"));
    }

    @Test
    void persisteElRatioYEsCompatibleConConfiguracionesAnteriores() throws Exception {
        ConfigManager manager = new ConfigManager(home);
        manager.save(AppConfig.builder().summaryInputRatio(0.5).build());
        assertEquals(0.5, manager.load().summaryInputRatio());
        Files.writeString(home.resolve("config.json"), "{\"chatModel\":\"modelo-local\"}");
        assertEquals(0.65, manager.load().summaryInputRatio());
        assertEquals("modelo-local", manager.load().chatModel());
    }

    @Test
    void rechazaRatiosInvalidos() {
        for (double value : new double[] {0, -0.1, 1, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> AppConfig.builder().summaryInputRatio(value).build());
        }
    }

    @Test
    void promptsPorDefectoYEnEspanolConservanLasRestricciones() {
        String original = new SummaryPromptBuilder(SummaryLanguage.DOCUMENT, false).build("datos", false);
        assertTrue(original.contains("idioma predominante del documento"));
        assertTrue(original.contains("No inventes"));
        assertTrue(original.contains("limitaciones solo si constan"));
        String spanish = new SummaryPromptBuilder(SummaryLanguage.SPANISH, false).build("data", true);
        assertTrue(spanish.contains("Responde en español"));
    }
}
