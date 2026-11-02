package dev.abyssace.kumo.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.abyssace.kumo.fake.FakeOllamaClient;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OllamaLlmClientTest {

    private static final String OPEN = "\u003cthinking\u003e";
    private static final String CLOSE = "\u003c/thinking\u003e";

    @Test
    void completeLimpiaElRazonamientoYEnviaLaConfiguracion() {
        FakeOllamaClient client = new FakeOllamaClient()
                .chatResponse("Hola " + OPEN + "secreto" + CLOSE + " mundo");
        OllamaLlmClient llm = new OllamaLlmClient(client, "qwen3:8b", 8192);

        String answer = llm.complete("pregunta");

        assertTrue(answer.contains("Hola"));
        assertTrue(answer.contains("mundo"));
        assertFalse(answer.contains("secreto"));
        assertEquals("qwen3:8b", client.lastChatModel());
        assertEquals(8192, client.lastNumCtx());
        assertEquals(1, client.lastMessages().size());
        assertEquals("pregunta", client.lastMessages().get(0).content());
        assertEquals("user", client.lastMessages().get(0).role());
        assertNull(client.lastFormat());
        assertFalse(client.lastStreaming());
    }

    @Test
    void streamFiltraTagsPartidosEntreTokens() {
        FakeOllamaClient client = new FakeOllamaClient()
                .streamChunks(List.of("Hola <thi", "nking>secreto</thi", "nking> mundo"));
        OllamaLlmClient llm = new OllamaLlmClient(client, "qwen3:8b", 4096);
        List<String> tokens = new ArrayList<>();

        llm.stream("pregunta", tokens::add);

        assertEquals("Hola  mundo", String.join("", tokens));
        assertEquals("qwen3:8b", client.lastChatModel());
        assertEquals(4096, client.lastNumCtx());
        assertTrue(client.lastStreaming());
    }

    @Test
    void streamDescartaElRazonamientoSinCerrar() {
        FakeOllamaClient client = new FakeOllamaClient()
                .streamChunks(List.of("Hola " + OPEN + "secreto"));
        OllamaLlmClient llm = new OllamaLlmClient(client, "qwen3:8b", 4096);
        List<String> tokens = new ArrayList<>();

        llm.stream("pregunta", tokens::add);

        assertEquals("Hola ", String.join("", tokens));
    }

    @Test
    void streamEmiteElPendienteSeguroAlTerminar() {
        FakeOllamaClient client = new FakeOllamaClient()
                .streamChunks(List.of("Hola <thi"));
        OllamaLlmClient llm = new OllamaLlmClient(client, "qwen3:8b", 4096);
        List<String> tokens = new ArrayList<>();

        llm.stream("pregunta", tokens::add);

        assertEquals("Hola <thi", String.join("", tokens));
    }

    @Test
    void completePropagaLosErroresDelCliente() {
        FakeOllamaClient client = new FakeOllamaClient()
                .failChat(new OllamaException("fallo de chat"));
        OllamaLlmClient llm = new OllamaLlmClient(client, "qwen3:8b", 4096);

        OllamaException error = assertThrows(OllamaException.class, () -> llm.complete("p"));

        assertEquals("fallo de chat", error.getMessage());
    }

    @Test
    void streamPropagaLosErroresDelCliente() {
        FakeOllamaClient client = new FakeOllamaClient()
                .failStream(new OllamaUnavailableException("sin conexión"));
        OllamaLlmClient llm = new OllamaLlmClient(client, "qwen3:8b", 4096);

        assertThrows(OllamaUnavailableException.class, () -> llm.stream("p", token -> {
        }));
    }

    @Test
    void validaLosArgumentos() {
        FakeOllamaClient client = new FakeOllamaClient();

        assertThrows(NullPointerException.class, () -> new OllamaLlmClient(null, "modelo", 1024));
        assertThrows(IllegalArgumentException.class, () -> new OllamaLlmClient(client, "  ", 1024));
        assertThrows(IllegalArgumentException.class, () -> new OllamaLlmClient(client, "modelo", 0));

        OllamaLlmClient llm = new OllamaLlmClient(client, "modelo", 1024);
        assertThrows(NullPointerException.class, () -> llm.complete(null));
        assertThrows(NullPointerException.class, () -> llm.stream(null, token -> {
        }));
        assertThrows(NullPointerException.class, () -> llm.stream("p", null));
    }
}
