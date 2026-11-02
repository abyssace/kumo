package dev.abyssace.kumo.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.fake.FakeOllamaClient;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OllamaEmbedderTest {

    @Test
    void embebePorLotesYConservaElOrden() {
        FakeOllamaClient client = new FakeOllamaClient()
                .embedHandler(inputs -> inputs.stream()
                        .map(text -> new float[] {text.length(), 1.0f})
                        .toList());
        OllamaEmbedder embedder = new OllamaEmbedder(client, "embeddinggemma", 2);

        List<float[]> vectors = embedder.embedAll(List.of("a", "bb", "ccc", "dddd", "eeeee"));

        assertEquals(5, vectors.size());
        assertEquals(List.of(2, 2, 1), client.embedBatches().stream().map(List::size).toList());
        assertEquals(1.0f, vectors.get(0)[0]);
        assertEquals(2.0f, vectors.get(1)[0]);
        assertEquals(3.0f, vectors.get(2)[0]);
        assertEquals(4.0f, vectors.get(3)[0]);
        assertEquals(5.0f, vectors.get(4)[0]);
    }

    @Test
    void embedDevuelveElPrimerVector() {
        FakeOllamaClient client = new FakeOllamaClient()
                .embedHandler(inputs -> List.of(new float[] {7.0f, 8.0f}));
        OllamaEmbedder embedder = new OllamaEmbedder(client, "embeddinggemma");

        float[] vector = embedder.embed("texto");

        assertEquals(7.0f, vector[0]);
        assertEquals(8.0f, vector[1]);
        assertEquals(1, client.embedBatches().size());
    }

    @Test
    void listaVaciaNoLlamaAlCliente() {
        FakeOllamaClient client = new FakeOllamaClient();
        OllamaEmbedder embedder = new OllamaEmbedder(client, "embeddinggemma");

        assertTrue(embedder.embedAll(List.of()).isEmpty());
        assertTrue(client.embedBatches().isEmpty());
    }

    @Test
    void fallaSiElClienteDevuelveUnNumeroIncorrectoDeVectores() {
        FakeOllamaClient client = new FakeOllamaClient()
                .embedHandler(inputs -> List.of(new float[] {1.0f, 0.0f}));
        OllamaEmbedder embedder = new OllamaEmbedder(client, "embeddinggemma");

        OllamaException error = assertThrows(
                OllamaException.class,
                () -> embedder.embedAll(List.of("uno", "dos")));

        assertTrue(error.getMessage().contains("2"));
    }

    @Test
    void fallaSiLosVectoresTienenDimensionesDistintas() {
        FakeOllamaClient client = new FakeOllamaClient()
                .embedHandler(inputs -> List.of(new float[] {1.0f, 0.0f}, new float[] {1.0f, 0.0f, 0.0f}));
        OllamaEmbedder embedder = new OllamaEmbedder(client, "embeddinggemma");

        OllamaException error = assertThrows(
                OllamaException.class,
                () -> embedder.embedAll(List.of("uno", "dos")));

        assertTrue(error.getMessage().contains("2"));
        assertTrue(error.getMessage().contains("3"));
    }

    @Test
    void fallaSiUnVectorEstaVacio() {
        FakeOllamaClient client = new FakeOllamaClient()
                .embedHandler(inputs -> List.of(new float[] {1.0f, 0.0f}, new float[0]));
        OllamaEmbedder embedder = new OllamaEmbedder(client, "embeddinggemma");

        assertThrows(OllamaException.class, () -> embedder.embedAll(List.of("uno", "dos")));
    }

    @Test
    void copiaLosVectoresDevueltos() {
        float[] original = {1.0f, 0.0f};
        FakeOllamaClient client = new FakeOllamaClient()
                .embedHandler(inputs -> List.of(original));
        OllamaEmbedder embedder = new OllamaEmbedder(client, "embeddinggemma");

        float[] vector = embedder.embed("texto");
        vector[0] = 99.0f;

        assertEquals(1.0f, original[0]);
    }

    @Test
    void validaLosArgumentos() {
        FakeOllamaClient client = new FakeOllamaClient();

        assertThrows(NullPointerException.class, () -> new OllamaEmbedder(null, "modelo"));
        assertThrows(IllegalArgumentException.class, () -> new OllamaEmbedder(client, "  "));
        assertThrows(IllegalArgumentException.class, () -> new OllamaEmbedder(client, "modelo", 0));

        OllamaEmbedder embedder = new OllamaEmbedder(client, "modelo");
        assertThrows(NullPointerException.class, () -> embedder.embedAll(null));
        List<String> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class, () -> embedder.embedAll(withNull));
        assertThrows(NullPointerException.class, () -> embedder.embed(null));
    }
}
