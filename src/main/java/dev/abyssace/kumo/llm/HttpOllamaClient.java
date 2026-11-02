package dev.abyssace.kumo.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.abyssace.kumo.i18n.Messages;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * Patrón: Adapter.
 *
 * <p>Cliente HTTP escrito a mano con {@code java.net.http.HttpClient}. Habla
 * exclusivamente con Ollama en localhost y no usa ningún framework de IA.
 * Implementa los cuatro endpoints que necesita kumo: {@code /api/tags},
 * {@code /api/show}, {@code /api/embed} y {@code /api/chat}.</p>
 *
 * <p>El chat siempre envía {@code think:false} en el nivel superior para
 * desactivar el razonamiento de Qwen3. La respuesta se limpia también en
 * {@link OllamaLlmClient} como red de seguridad.</p>
 */
public final class HttpOllamaClient implements OllamaClient {

    private static final int MAX_ERROR_BODY = 4096;

    private final String baseUrl;
    private final Duration requestTimeout;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public HttpOllamaClient(String baseUrl) {
        this(baseUrl, Duration.ofMinutes(5));
    }

    public HttpOllamaClient(String baseUrl, Duration requestTimeout) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("requestTimeout debe ser positivo");
        }
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public List<OllamaModel> listModels() {
        HttpRequest request = request("/api/tags").GET().build();
        TagsResponse response = sendJson(request, TagsResponse.class, "/api/tags");
        if (response == null || response.models() == null) {
            return List.of();
        }

        List<OllamaModel> models = new ArrayList<>();
        for (Tag tag : response.models()) {
            if (tag == null) {
                continue;
            }
            String name = tag.name() != null && !tag.name().isBlank() ? tag.name() : tag.model();
            if (name != null && !name.isBlank()) {
                models.add(new OllamaModel(name, tag.size()));
            }
        }
        return List.copyOf(models);
    }

    @Override
    public OllamaModelInfo showModel(String model) {
        Objects.requireNonNull(model, "model");
        String body = writeJson(new ShowRequest(model));
        HttpRequest request = request("/api/show")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        ShowResponse response = sendJson(request, ShowResponse.class, "/api/show");
        if (response == null) {
            throw new OllamaException(Messages.OLLAMA_BAD_RESPONSE.formatted("/api/show", "respuesta vacía"));
        }
        List<String> capabilities = response.capabilities() == null ? List.of() : response.capabilities();
        return new OllamaModelInfo(model, contextLength(response.modelInfo()), capabilities);
    }

    @Override
    public List<float[]> embed(String model, List<String> inputs) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(inputs, "inputs");
        if (inputs.isEmpty()) {
            return List.of();
        }

        String body = writeJson(new EmbedRequest(model, List.copyOf(inputs), false));
        HttpRequest request = request("/api/embed")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        EmbedResponse response = sendJson(request, EmbedResponse.class, "/api/embed");
        if (response == null || response.embeddings() == null
                || response.embeddings().size() != inputs.size()) {
            throw new OllamaException(Messages.OLLAMA_EMPTY_EMBEDDINGS.formatted(model, inputs.size()));
        }

        List<float[]> vectors = new ArrayList<>(response.embeddings().size());
        for (float[] vector : response.embeddings()) {
            if (vector == null || vector.length == 0) {
                throw new OllamaException(Messages.OLLAMA_EMPTY_EMBEDDINGS.formatted(model, inputs.size()));
            }
            vectors.add(vector.clone());
        }
        return List.copyOf(vectors);
    }

    @Override
    public String chat(String model, List<OllamaMessage> messages, int numCtx, String formatJson) {
        validateChatArgs(model, messages, numCtx);
        JsonNode format = parseFormat(formatJson);
        ChatRequest payload = new ChatRequest(
                model, List.copyOf(messages), false, false, new ChatOptions(numCtx), format);
        String body = writeJson(payload);
        HttpRequest request = request("/api/chat")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        ChatResponse response = sendJson(request, ChatResponse.class, "/api/chat");
        if (response == null) {
            throw new OllamaException(Messages.OLLAMA_BAD_RESPONSE.formatted("/api/chat", "respuesta vacía"));
        }
        if (response.error() != null && !response.error().isBlank()) {
            throw new OllamaException("Ollama devolvió un error: " + response.error());
        }
        if (response.message() == null || response.message().content() == null) {
            throw new OllamaException(
                    Messages.OLLAMA_BAD_RESPONSE.formatted("/api/chat", "respuesta sin contenido"));
        }
        return response.message().content();
    }

    @Override
    public void chatStreaming(
            String model, List<OllamaMessage> messages, int numCtx, Consumer<String> onToken) {
        validateChatArgs(model, messages, numCtx);
        Objects.requireNonNull(onToken, "onToken");

        ChatRequest payload = new ChatRequest(
                model, List.copyOf(messages), true, false, new ChatOptions(numCtx), null);
        String body = writeJson(payload);
        HttpRequest request = request("/api/chat")
                .setHeader("Accept", "application/x-ndjson")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> response = sendStream(request);
        try (InputStream in = response.body();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new OllamaException(Messages.OLLAMA_HTTP_ERROR.formatted(
                        response.statusCode(), "/api/chat", readLimited(reader)));
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                ChatResponse chunk;
                try {
                    chunk = mapper.readValue(line, ChatResponse.class);
                } catch (IOException | RuntimeException ex) {
                    throw new OllamaException(
                            Messages.OLLAMA_BAD_RESPONSE.formatted("/api/chat", ex.getMessage()), ex);
                }
                if (chunk == null) {
                    continue;
                }
                if (chunk.error() != null && !chunk.error().isBlank()) {
                    throw new OllamaException("Ollama devolvió un error: " + chunk.error());
                }
                if (chunk.message() != null
                        && chunk.message().content() != null
                        && !chunk.message().content().isEmpty()) {
                    onToken.accept(chunk.message().content());
                }
                if (chunk.done()) {
                    break;
                }
            }
        } catch (IOException ex) {
            throw new OllamaException(
                    Messages.OLLAMA_BAD_RESPONSE.formatted("/api/chat", ex.getMessage()), ex);
        }
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(requestTimeout)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .header("User-Agent", "kumo");
    }

    private <T> T sendJson(HttpRequest request, Class<T> type, String path) {
        HttpResponse<String> response = sendString(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new OllamaException(Messages.OLLAMA_HTTP_ERROR.formatted(
                    response.statusCode(), path, truncate(response.body())));
        }
        try {
            return mapper.readValue(response.body(), type);
        } catch (IOException | RuntimeException ex) {
            throw new OllamaException(
                    Messages.OLLAMA_BAD_RESPONSE.formatted(path, ex.getMessage()), ex);
        }
    }

    private HttpResponse<String> sendString(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (ConnectException | HttpConnectTimeoutException ex) {
            throw new OllamaUnavailableException(Messages.OLLAMA_UNAVAILABLE.formatted(baseUrl), ex);
        } catch (HttpTimeoutException ex) {
            throw new OllamaException(
                    "Ollama tardó demasiado en responder (" + baseUrl + ").", ex);
        } catch (IOException ex) {
            throw new OllamaUnavailableException(Messages.OLLAMA_UNAVAILABLE.formatted(baseUrl), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OllamaException(Messages.OLLAMA_REQUEST_INTERRUPTED, ex);
        }
    }

    private HttpResponse<InputStream> sendStream(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (ConnectException | HttpConnectTimeoutException ex) {
            throw new OllamaUnavailableException(Messages.OLLAMA_UNAVAILABLE.formatted(baseUrl), ex);
        } catch (HttpTimeoutException ex) {
            throw new OllamaException(
                    "Ollama tardó demasiado en responder (" + baseUrl + ").", ex);
        } catch (IOException ex) {
            throw new OllamaUnavailableException(Messages.OLLAMA_UNAVAILABLE.formatted(baseUrl), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OllamaException(Messages.OLLAMA_REQUEST_INTERRUPTED, ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new OllamaException(
                    Messages.OLLAMA_BAD_RESPONSE.formatted("petición", ex.getMessage()), ex);
        }
    }

    private JsonNode parseFormat(String formatJson) {
        if (formatJson == null || formatJson.isBlank()) {
            return null;
        }
        try {
            return mapper.readTree(formatJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(
                    "El esquema JSON de formato no es válido: " + ex.getMessage(), ex);
        }
    }

    private static OptionalInt contextLength(Map<String, JsonNode> modelInfo) {
        if (modelInfo == null) {
            return OptionalInt.empty();
        }
        for (Map.Entry<String, JsonNode> entry : modelInfo.entrySet()) {
            if (!entry.getKey().endsWith(".context_length")) {
                continue;
            }
            JsonNode value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value.isIntegralNumber() && value.canConvertToInt()) {
                return OptionalInt.of(value.asInt());
            }
            if (value.isTextual()) {
                try {
                    return OptionalInt.of(Integer.parseInt(value.asText()));
                } catch (NumberFormatException ignored) {
                    // Se prueba con otra clave, si existe.
                }
            }
        }
        return OptionalInt.empty();
    }

    private static void validateChatArgs(String model, List<OllamaMessage> messages, int numCtx) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(messages, "messages");
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages no puede estar vacío");
        }
        if (numCtx < 1) {
            throw new IllegalArgumentException("numCtx debe ser >= 1, pero fue " + numCtx);
        }
    }

    private static String readLimited(BufferedReader reader) throws IOException {
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[1024];
        int read;
        while (result.length() < MAX_ERROR_BODY
                && (read = reader.read(buffer, 0, Math.min(buffer.length, MAX_ERROR_BODY - result.length()))) != -1) {
            result.append(buffer, 0, read);
        }
        return result.toString().strip();
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        String stripped = body.strip();
        return stripped.length() <= MAX_ERROR_BODY
                ? stripped
                : stripped.substring(0, MAX_ERROR_BODY) + "…";
    }

    private static String normalizeBaseUrl(String value) {
        Objects.requireNonNull(value, "baseUrl");
        String trimmed = value.strip();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getPath();
            if (scheme == null
                    || host == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || !isLoopback(host)
                    || (path != null && !path.isEmpty() && !path.equals("/"))
                    || uri.getQuery() != null
                    || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    Messages.OLLAMA_INVALID_BASE_URL.formatted(value), ex);
        }
        return trimmed;
    }

    private static boolean isLoopback(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.equals("localhost")
                || normalized.equals("127.0.0.1")
                || normalized.equals("::1")
                || normalized.equals("[::1]");
    }

    // --- DTOs privados del protocolo HTTP ----------------------------------

    private record ShowRequest(String model) {
    }

    private record EmbedRequest(String model, List<String> input, boolean truncate) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ChatRequest(
            String model,
            List<OllamaMessage> messages,
            boolean stream,
            boolean think,
            ChatOptions options,
            JsonNode format) {
    }

    private record ChatOptions(@JsonProperty("num_ctx") int numCtx) {
    }

    private record TagsResponse(List<Tag> models) {
    }

    private record Tag(String name, String model, long size) {
    }

    private record ShowResponse(
            @JsonProperty("model_info") Map<String, JsonNode> modelInfo,
            List<String> capabilities) {
    }

    private record EmbedResponse(List<float[]> embeddings) {
    }

    private record ChatResponse(
            OllamaMessage message,
            boolean done,
            String error,
            @JsonProperty("done_reason") String doneReason) {
    }
}
