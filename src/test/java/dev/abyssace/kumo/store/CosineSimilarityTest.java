package dev.abyssace.kumo.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CosineSimilarityTest {

    private final CosineSimilarity cosine = new CosineSimilarity();

    @Test
    void vectoresIdenticosDanUno() {
        assertEquals(1.0, cosine.score(new float[] {1, 2, 3}, new float[] {1, 2, 3}), 1e-9);
    }

    @Test
    void vectoresOrtogonalesDanCero() {
        assertEquals(0.0, cosine.score(new float[] {1, 0}, new float[] {0, 1}), 1e-9);
    }

    @Test
    void vectoresOpuestosDanMenosUno() {
        assertEquals(-1.0, cosine.score(new float[] {1, 0}, new float[] {-1, 0}), 1e-9);
    }

    @Test
    void vectorNuloDaCero() {
        assertEquals(0.0, cosine.score(new float[] {0, 0}, new float[] {1, 2}), 1e-9);
    }

    @Test
    void dimensionesDistintasFalla() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> cosine.score(new float[] {1, 0}, new float[] {1, 0, 0}));

        assertTrue(error.getMessage().contains("dimensiones"));
    }

    @Test
    void nombreEsCoseno() {
        assertEquals("coseno", cosine.name());
    }
}
