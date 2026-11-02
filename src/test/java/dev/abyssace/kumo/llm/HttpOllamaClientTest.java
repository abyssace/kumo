package dev.abyssace.kumo.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.fake.FakeOllamaServer;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpOllamaClientTest {

    private FakeOllamaServer server;
    private HttpOllamaClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new FakeOllamaServer();
        client = new HttpOllamaClient(server.baseUrl());
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void listModelsLeeTagsYNormalizaLosNombres() {
        server.modelNames(List.of("qwen3:8b", "embeddinggemma:latest"));

        List<OllamaModel> models = client.listModels();

        assertEquals(2, models.size());
        assertEquals("qwen3:8b", models.get(0).name());
        assertEquals("embeddinggemma:latest", models.get(1).name());
        assertEquals(100, models.get(0).size());
        assertEquals("GET", server.lastRequest("/api/tags").orElseThrow().method());
    }

    @Test
    void listModelsDevuelveVacioSiLaRespuestaNoTraeModelos() {
        server.tagsBodyOverride("{}");

        assertTrue(client.listModels().isEmpty());
    }

    @Test
    void listModelsConErrorHttpFalla() {
        server.tagsStatus(500);

        OllamaException error = assertThrows(OllamaException.class, client::listModels);

        assertTrue(error.getMessage().contains("500"));
        assertTrue(error.getMessage().contains("/api/tags"));
    }

    @Test
    void showModelExtraeElContextoYLasCapacidades() {
        server.showContextLength(8192);

        OllamaModelInfo info = client.showModel("embeddinggemma");

        assertEquals("embeddinggemma", info.name());
        assertEquals(8192, info.contextLength().getAsInt());
        assertTrue(info.supportsEmbedding());
        assertTrue(server.lastRequest("/api/show").orElseThrow().body().contains("\"model\":\"embeddinggemma\""));
    }

    @Test
    void showModelAceptaElContextoComoTexto() {
        server.showBodyOverride(
                "{\"model_info\":{\"llama.context_length\":\"4096\"},\"capabilities\":[]}");

        OllamaModelInfo info = client.showModel("embeddinggemma");

        assertEquals(4096, info.contextLength().getAsInt());
    }

    @Test
    void showModelSinContextoDevuelveOptionalVacio() {
        server.showContextLength(null);

        assertTrue(client.showModel("embeddinggemma").contextLength().isEmpty());
    }

    @Test
    void showModelConRespuestaVaciaFalla() {
        server.showBodyOverride("null");

        OllamaException error = assertThrows(OllamaException.class, () -> client.showModel("modelo"));

        assertTrue(error.getMessage().contains("respuesta vacía"));
    }

    @Test
    void embedEnviaTruncateFalseYDevuelveLosVectores() {
        server.embeddingDimensions(4);

        List<float[]> vectors = client.embed("embeddinggemma", List.of("uno", "dos"));

        assertEquals(2, vectors.size());
        assertEquals(4, vectors.get(0).length);
        assertEquals(1.0f, vectors.get(0)[0]);
        String body = server.lastRequest("/api/embed").orElseThrow().body();
        assertTrue(body.contains("\"truncate\":false"));
        assertTrue(body.contains("\"model\":\"embeddinggemma\""));
        assertTrue(body.contains("\"input\":[\"uno\",\"dos\"]"));
    }

    @Test
    void embedConListaVaciaNoHacePeticion() {
        assertTrue(client.embed("embeddinggemma", List.of()).isEmpty());
        assertTrue(server.requests().isEmpty());
    }

    @Test
    void embedConNumeroIncorrectoDeVectoresFalla() {
        server.embeddingOverride(List.of(new float[] {1.0f, 0.0f}));

        OllamaException error = assertThrows(
                OllamaException.class,
                () -> client.embed("embeddinggemma", List.of("uno", "dos")));

        assertTrue(error.getMessage().contains("2"));
    }

    @Test
    void embedConUnVectorVacioFalla() {
        server.embeddingOverride(List.of(new float[0], new float[0]));

        assertThrows(
                OllamaException.class,
                () -> client.embed("embeddinggemma", List.of("uno", "dos")));
    }

    @Test
    void embedSinEmbeddingsEnLaRespuestaFalla() {
        server.embedBodyOverride("{}");

        assertThrows(
                OllamaException.class,
                () -> client.embed("embeddinggemma", List.of("uno")));
    }

    @Test
    void chatEnviaThinkFalseYDevuelveElContenido() {
        server.chatResponse("respuesta completa");

        String answer = client.chat("qwen3:8b", List.of(OllamaMessage.user("hola")), 8192, null);

        assertEquals("respuesta completa", answer);
        String body = server.lastRequest("/api/chat").orElseThrow().body();
        assertTrue(body.contains("\"think\":false"));
        assertTrue(body.contains("\"stream\":false"));
        assertTrue(body.contains("\"num_ctx\":8192"));
        assertFalse(body.contains("\"format\""));
    }

    @Test
    void chatConFormatoJsonIncluyeElEsquema() {
        client.chat("qwen3:8b", List.of(OllamaMessage.user("hola")), 4096, "{\"type\":\"object\"}");

        String body = server.lastRequest("/api/chat").orElseThrow().body();
        assertTrue(body.contains("\"format\":{\"type\":\"object\"}"));
    }

    @Test
    void chatConFormatoInvalidoFallaSinHacerPeticion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> client.chat("qwen3:8b", List.of(OllamaMessage.user("hola")), 4096, "{no json"));

        assertTrue(server.requests().isEmpty());
    }

    @Test
    void chatConErrorEnElCuerpoFalla() {
        server.chatBodyOverride("{\"error\":\"modelo no encontrado\"}");

        OllamaException error = assertThrows(
                OllamaException.class,
                () -> client.chat("qwen3:8b", List.of(OllamaMessage.user("hola")), 4096, null));

        assertTrue(error.getMessage().contains("modelo no encontrado"));
    }

    @Test
    void chatSinContenidoFalla() {
        server.chatBodyOverride("{\"message\":null,\"done\":true}");

        OllamaException error = assertThrows(
                OllamaException.class,
                () -> client.chat("qwen3:8b", List.of(OllamaMessage.user("hola")), 4096, null));

        assertTrue(error.getMessage().contains("sin contenido"));
    }

    @Test
    void chatConErrorHttpFalla() {
        server.chatStatus(500);

        OllamaException error = assertThrows(
                OllamaException.class,
                () -> client.chat("qwen3:8b", List.of(OllamaMessage.user("hola")), 4096, null));

        assertTrue(error.getMessage().contains("500"));
    }

    @Test
    void chatStreamingLeeNdjsonYEnviaLaConfiguracion() {
        server.streamChunks(List.of("Hola ", "mundo"));
        List<String> tokens = new ArrayList<>();

        client.chatStreaming("qwen3:8b", List.of(OllamaMessage.user("pregunta")), 4096, tokens::add);

        assertEquals(List.of("Hola ", "mundo"), tokens);
        String body = server.lastRequest("/api/chat").orElseThrow().body();
        assertTrue(body.contains("\"stream\":true"));
        assertTrue(body.contains("\"think\":false"));
        assertTrue(body.contains("\"num_ctx\":4096"));
    }

    @Test
    void chatStreamingConErrorHttpFalla() {
        server.chatStatus(500);

        assertThrows(
                OllamaException.class,
                () -> client.chatStreaming(
                        "qwen3:8b", List.of(OllamaMessage.user("hola")), 4096, token -> {
                        }));
    }

    @Test
    void chatStreamingConUnaLineaInvalidaFalla() {
        server.streamBodyOverride("esto no es json\n");

        assertThrows(
                OllamaException.class,
                () -> client.chatStreaming(
                        "qwen3:8b", List.of(OllamaMessage.user("hola")), 4096, token -> {
                        }));
    }

    @Test
    void chatStreamingConUnErrorEnLaLineaFalla() {
        server.streamBodyOverride("{\"error\":\"fallo de generación\"}\n");

        OllamaException error = assertThrows(
                OllamaException.class,
                () -> client.chatStreaming(
                        "qwen3:8b", List.of(OllamaMessage.user("hola")), 4096, token -> {
                        }));

        assertTrue(error.getMessage().contains("fallo de generación"));
    }

    @Test
    void validaLaUrlBase() {
        assertThrows(NullPointerException.class, () -> new HttpOllamaClient(null));
        assertThrows(IllegalArgumentException.class, () -> new HttpOllamaClient("http://example.com:11434"));
        assertThrows(IllegalArgumentException.class, () -> new HttpOllamaClient("ftp://localhost:11434"));
        assertThrows(IllegalArgumentException.class, () -> new HttpOllamaClient("http://localhost:11434/otra"));
        assertThrows(IllegalArgumentException.class, () -> new HttpOllamaClient("http://localhost:11434?x=1"));
        assertThrows(IllegalArgumentException.class, () -> new HttpOllamaClient("http://localhost:11434#frag"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HttpOllamaClient("http://localhost:11434", Duration.ZERO));

        new HttpOllamaClient("http://localhost:11434/");
        new HttpOllamaClient("  http://127.0.0.1:11434  ");
    }

    @Test
    void validaLosArgumentosDeChat() {
        assertThrows(NullPointerException.class, () -> client.showModel(null));
        assertThrows(NullPointerException.class, () -> client.embed(null, List.of()));
        assertThrows(NullPointerException.class, () -> client.embed("modelo", null));
        assertThrows(
                NullPointerException.class,
                () -> client.chat(null, List.of(OllamaMessage.user("hola")), 1, null));
        assertThrows(NullPointerException.class, () -> client.chat("modelo", null, 1, null));
        assertThrows(IllegalArgumentException.class, () -> client.chat("modelo", List.of(), 1, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> client.chat("modelo", List.of(OllamaMessage.user("hola")), 0, null));
        assertThrows(
                NullPointerException.class,
                () -> client.chatStreaming(
                        "modelo", List.of(OllamaMessage.user("hola")), 1, null));
    }
}
