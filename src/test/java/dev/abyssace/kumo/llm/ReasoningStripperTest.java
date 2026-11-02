package dev.abyssace.kumo.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReasoningStripperTest {

    @Test
    void eliminaLasEtiquetasRealesDeQwenYLosBloquesAbiertos() {
        assertEquals("respuesta", ReasoningStripper.strip(
                "\u003cthink\u003erazonamiento\u003c/think\u003erespuesta"));
        assertEquals("antes", ReasoningStripper.strip("antes\u003cthink\u003esecreto"));
    }

    private static final String OPEN = "\u003cthinking\u003e";
    private static final String CLOSE = "\u003c/thinking\u003e";

    @Test
    void eliminaUnBloqueCompleto() {
        String result = ReasoningStripper.strip("Antes " + OPEN + "secreto" + CLOSE + " después");

        assertTrue(result.contains("Antes"));
        assertTrue(result.contains("después"));
        assertFalse(result.contains("secreto"));
    }

    @Test
    void eliminaLaVarianteConEtiquetasCompletas() {
        String result = ReasoningStripper.strip("Hola " + OPEN + "pienso" + CLOSE + " mundo");

        assertEquals("Hola  mundo", result);
    }

    @Test
    void eliminaVariosBloques() {
        String result = ReasoningStripper.strip(
                "a " + OPEN + "uno" + CLOSE + "b " + OPEN + "dos" + CLOSE + " c");

        assertEquals("a b  c", result);
    }

    @Test
    void descartaUnBloqueSinCerrar() {
        assertEquals("Hola", ReasoningStripper.strip("Hola " + OPEN + "me quedo pensando"));
    }

    @Test
    void recortaLosExtremos() {
        assertEquals("respuesta", ReasoningStripper.strip("   respuesta   "));
    }

    @Test
    void textoNuloOVacioDevuelveCadenaVacia() {
        assertEquals("", ReasoningStripper.strip(null));
        assertEquals("", ReasoningStripper.strip(""));
    }

    @Test
    void conservaElTextoNormal() {
        assertEquals("una respuesta normal", ReasoningStripper.strip("una respuesta normal"));
    }
}
