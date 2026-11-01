package dev.abyssace.kumo.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SourceLocationTest {

    @Test
    void describeUnaPagina() {
        assertEquals("página 3", new SourceLocation.PageRef(3).describe());
    }

    @Test
    void describeUnRangoDeLineas() {
        assertEquals("líneas 4-9", new SourceLocation.LineRange(4, 9).describe());
        assertEquals("línea 4", new SourceLocation.LineRange(4, 4).describe());
    }

    @Test
    void noneNoTieneDescripcion() {
        assertEquals("", SourceLocation.none().describe());
    }

    @Test
    void rechazaPaginaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation.PageRef(0));
    }

    @Test
    void rechazaRangoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation.LineRange(5, 4));
        assertThrows(IllegalArgumentException.class, () -> new SourceLocation.LineRange(0, 4));
    }
}
