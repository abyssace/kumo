package dev.abyssace.kumo.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ReasoningFilterTest {

    @Test
    void filtraLasEtiquetasRealesDeQwenCaracterACaracter() {
        var filter = new ReasoningFilter();
        String raw = "antes\u003cthink\u003esecreto\u003c/think\u003edespués";
        StringBuilder result = new StringBuilder();
        raw.chars().forEach(c -> result.append(filter.feed(String.valueOf((char) c))));
        result.append(filter.finish());
        assertEquals("antesdespués", result.toString());
    }

    private static final String OPEN = "\u003cthinking\u003e";
    private static final String CLOSE = "\u003c/thinking\u003e";

    @Test
    void dejaPasarTextoNormal() {
        ReasoningFilter filter = new ReasoningFilter();

        assertEquals("Hola", filter.feed("Hola"));
        assertEquals(" mundo", filter.feed(" mundo"));
        assertEquals("", filter.finish());
    }

    @Test
    void filtraUnTagPartidoEntreTokens() {
        ReasoningFilter filter = new ReasoningFilter();

        assertEquals("Hola ", filter.feed("Hola <thi"));
        assertEquals("", filter.feed("nking>secreto</thi"));
        assertEquals(" mundo", filter.feed("nking> mundo"));
        assertEquals("", filter.finish());
    }

    @Test
    void filtraUnTagCompletoEnUnSoloToken() {
        ReasoningFilter filter = new ReasoningFilter();

        assertEquals("Hola  mundo", filter.feed("Hola " + OPEN + "secreto" + CLOSE + " mundo"));
        assertEquals("", filter.finish());
    }

    @Test
    void filtraLaVarianteConEtiquetasCompletas() {
        ReasoningFilter filter = new ReasoningFilter();

        assertEquals("A  B", filter.feed("A " + OPEN + "x" + CLOSE + " B"));
        assertEquals("", filter.finish());
    }

    @Test
    void descartaElRazonamientoSinCerrarAlTerminar() {
        ReasoningFilter filter = new ReasoningFilter();

        assertEquals("Hola ", filter.feed("Hola " + OPEN + "secreto"));
        assertEquals("", filter.finish());
    }

    @Test
    void alTerminarDevuelveUnTagParcialQueNoEraTag() {
        ReasoningFilter filter = new ReasoningFilter();

        assertEquals("Hola ", filter.feed("Hola <thi"));
        assertEquals("<thi", filter.finish());
    }

    @Test
    void finishEsIdempotenteYBloqueaNuevosFeeds() {
        ReasoningFilter filter = new ReasoningFilter();
        filter.feed("Hola");

        assertEquals("", filter.finish());
        assertEquals("", filter.finish());
        assertThrows(IllegalStateException.class, () -> filter.feed("más"));
    }

    @Test
    void ignoraFragmentosNulosOVacios() {
        ReasoningFilter filter = new ReasoningFilter();

        assertEquals("", filter.feed(null));
        assertEquals("", filter.feed(""));
        assertEquals("", filter.finish());
    }
}
