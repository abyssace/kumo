package dev.abyssace.kumo.fake;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Patrón: Test Double (Fake) de la API HTTP de Ollama.
 *
 * <p>Levanta un {@link HttpServer} real en loopback para probar
 * {@code HttpOllamaClient} y la CLI sin instalar Ollama. Registra las peticiones
 * y permite simular respuestas, errores y streaming NDJSON.</p>
 */
public final class FakeOllamaServer implements AutoCloseable {

    public record RecordedRequest(String method, String path, String body) {
    }

    private final HttpServer server;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();

    private volatile List<String> modelNames = List.of("qwen3:8b", "embeddinggemma:latest");
    private volatile Integer showContextLength = 2048;
    private volatile List<String> showCapabilities = List.of("embedding");
    private volatile int embeddingDimensions = 3;
    private volatile List<float[]> embeddingOverride;
    private volatile String chatResponse = "respuesta del modelo";
    private volatile List<String> streamChunks = List.of("respuesta ", "del modelo");

    private volatile int tagsStatus = 200;
    private volatile int showStatus = 200;
    private volatile int embedStatus = 200;
    private volatile int chatStatus = 200;
    private volatile String errorBody = "{\"error\":\"fallo simulado\"}";
    private volatile String tagsBodyOverride;
    private volatile String showBodyOverride;
    private volatile String embedBodyOverride;
    private volatile String chatBodyOverride;
    private volatile String streamBodyOverride;

    public FakeOllamaServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        this.server.createContext("/", this::handle);
        this.server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public List<RecordedRequest> requests() {
        return List.copyOf(requests);
    }

    public Optional<RecordedRequest> lastRequest(String path) {
        List<RecordedRequest> snapshot = requests();
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            if (snapshot.get(i).path().equals(path)) {
                return Optional.of(snapshot.get(i));
            }
        }
        return Optional.empty();
    }

    public long countRequests(String path) {
        return requests.stream().filter(request -> request.path().equals(path)).count();
    }

    public FakeOllamaServer modelNames(List<String> value) {
        this.modelNames = List.copyOf(value);
        return this;
    }

    public FakeOllamaServer showContextLength(Integer value) {
        this.showContextLength = value;
        return this;
    }

    public FakeOllamaServer showCapabilities(List<String> value) {
        this.showCapabilities = List.copyOf(value);
        return this;
    }

    public FakeOllamaServer embeddingDimensions(int value) {
        this.embeddingDimensions = value;
        return this;
    }

    public FakeOllamaServer embeddingOverride(List<float[]> value) {
        this.embeddingOverride = value == null ? null : List.copyOf(value);
        return this;
    }

    public FakeOllamaServer chatResponse(String value) {
        this.chatResponse = value;
        return this;
    }

    public FakeOllamaServer streamChunks(List<String> value) {
        this.streamChunks = List.copyOf(value);
        return this;
    }

    public FakeOllamaServer tagsStatus(int value) {
        this.tagsStatus = value;
        return this;
    }

    public FakeOllamaServer showStatus(int value) {
        this.showStatus = value;
        return this;
    }

    public FakeOllamaServer embedStatus(int value) {
        this.embedStatus = value;
        return this;
    }

    public FakeOllamaServer chatStatus(int value) {
        this.chatStatus = value;
        return this;
    }

    public FakeOllamaServer errorBody(String value) {
        this.errorBody = value;
        return this;
    }

    public FakeOllamaServer tagsBodyOverride(String value) {
        this.tagsBodyOverride = value;
        return this;
    }

    public FakeOllamaServer showBodyOverride(String value) {
        this.showBodyOverride = value;
        return this;
    }

    public FakeOllamaServer embedBodyOverride(String value) {
        this.embedBodyOverride = value;
        return this;
    }

    public FakeOllamaServer chatBodyOverride(String value) {
        this.chatBodyOverride = value;
        return this;
    }

    public FakeOllamaServer streamBodyOverride(String value) {
        this.streamBodyOverride = value;
        return this;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(new RecordedRequest(exchange.getRequestMethod(), path, body));

            switch (path) {
                case "/api/tags" -> tags(exchange);
                case "/api/show" -> show(exchange);
                case "/api/embed" -> embed(exchange, body);
                case "/api/chat" -> chat(exchange, body);
                default -> send(exchange, 404, "{\"error\":\"ruta desconocida\"}");
            }
        } catch (RuntimeException ex) {
            // El servidor de pruebas no debe tumbar el test con una traza.
            send(exchange, 500, "{\"error\":\"" + ex.getMessage() + "\"}");
        }
    }

    private void tags(HttpExchange exchange) throws IOException {
        if (tagsStatus != 200) {
            send(exchange, tagsStatus, errorBody);
            return;
        }
        if (tagsBodyOverride != null) {
            send(exchange, 200, tagsBodyOverride);
            return;
        }
        List<Object> models = new ArrayList<>();
        long size = 100;
        for (String name : modelNames) {
            models.add(mapper.createObjectNode()
                    .put("name", name)
                    .put("model", name)
                    .put("size", size++));
        }
        send(exchange, 200, mapper.writeValueAsString(
                mapper.createObjectNode().set("models", mapper.valueToTree(models))));
    }

    private void show(HttpExchange exchange) throws IOException {
        if (showStatus != 200) {
            send(exchange, showStatus, errorBody);
            return;
        }
        if (showBodyOverride != null) {
            send(exchange, 200, showBodyOverride);
            return;
        }
        var root = mapper.createObjectNode();
        if (showContextLength != null) {
            root.set("model_info", mapper.createObjectNode().put("llama.context_length", showContextLength));
        }
        root.set("capabilities", mapper.valueToTree(showCapabilities));
        send(exchange, 200, mapper.writeValueAsString(root));
    }

    private void embed(HttpExchange exchange, String body) throws IOException {
        if (embedStatus != 200) {
            send(exchange, embedStatus, errorBody);
            return;
        }
        if (embedBodyOverride != null) {
            send(exchange, 200, embedBodyOverride);
            return;
        }
        int count = mapper.readTree(body).path("input").size();
        List<float[]> vectors = new ArrayList<>();
        if (embeddingOverride != null) {
            vectors.addAll(embeddingOverride);
        } else {
            for (int i = 0; i < count; i++) {
                float[] vector = new float[embeddingDimensions];
                vector[0] = 1.0f;
                vectors.add(vector);
            }
        }
        send(exchange, 200, mapper.writeValueAsString(
                mapper.createObjectNode().set("embeddings", mapper.valueToTree(vectors))));
    }

    private void chat(HttpExchange exchange, String body) throws IOException {
        if (chatStatus != 200) {
            send(exchange, chatStatus, errorBody);
            return;
        }
        JsonNode request = mapper.readTree(body);
        if (request.path("stream").asBoolean(false)) {
            stream(exchange, request);
            return;
        }
        if (chatBodyOverride != null) {
            send(exchange, 200, chatBodyOverride);
            return;
        }
        var response = mapper.createObjectNode();
        response.set("message", mapper.createObjectNode()
                .put("role", "assistant")
                .put("content", chatResponse));
        response.put("done", true);
        send(exchange, 200, mapper.writeValueAsString(response));
    }

    private void stream(HttpExchange exchange, JsonNode request) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/x-ndjson; charset=utf-8");
        exchange.sendResponseHeaders(200, 0);
        try (OutputStream out = exchange.getResponseBody()) {
            if (streamBodyOverride != null) {
                out.write(streamBodyOverride.getBytes(StandardCharsets.UTF_8));
                out.flush();
                return;
            }
            for (String chunk : streamChunks) {
                var line = mapper.createObjectNode();
                line.set("message", mapper.createObjectNode()
                        .put("role", "assistant")
                        .put("content", chunk));
                line.put("done", false);
                out.write((mapper.writeValueAsString(line) + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
            var last = mapper.createObjectNode();
            last.set("message", mapper.createObjectNode()
                    .put("role", "assistant")
                    .put("content", ""));
            last.put("done", true);
            out.write((mapper.writeValueAsString(last) + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
