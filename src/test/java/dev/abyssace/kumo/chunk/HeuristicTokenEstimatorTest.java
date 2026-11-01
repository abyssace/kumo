package dev.abyssace.kumo.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HeuristicTokenEstimatorTest {

    private final HeuristicTokenEstimator estimator = new HeuristicTokenEstimator();

    @Test
    void estimaCuatroCaracteresPorToken() {
        assertEquals(3, estimator.estimate("123456789012"));
        assertEquals(4, estimator.estimate("1234567890123456"));
    }

    @Test
    void redondeaHaciaArriba() {
        assertEquals(1, estimator.estimate("a"));
        assertEquals(2, estimator.estimate("abcde"));
    }

    @Test
    void textoVacioOSoloEspaciosDaCero() {
        assertEquals(0, estimator.estimate(""));
        assertEquals(0, estimator.estimate("   \n\t "));
    }

    @Test
    void rechazaTextoNulo() {
        assertThrows(NullPointerException.class, () -> estimator.estimate(null));
    }
}
