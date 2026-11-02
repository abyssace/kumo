package dev.abyssace.kumo.fake;

import dev.abyssace.kumo.llm.OllamaClient;
import dev.abyssace.kumo.llm.OllamaMessage;
import dev.abyssace.kumo.llm.OllamaModel;
import dev.abyssace.kumo.llm.OllamaModelInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Patrón: Test Double (Fake) de {@link OllamaClient}.
 *
 * <p>Permite probar la capa LLM sin abrir un puerto. Registra las llamadas y
 * deja configurar respuestas y errores. El servidor HTTP falso se usa aparte
 * para probar el adaptador real.</p>
 */
public final class FakeOllamaClient implements OllamaClient {

    private List<OllamaModel> models = List.of(
            new OllamaModel("qwen3:8b", 100),
            new OllamaModel("embeddinggemma:latest", 50));
    private OptionalInt contextLength = OptionalInt.empty();
    private List<String> capabilities = List.of("embedding");
    private Function<List<String>, List<float[]>> embedHandler = this::defaultVectors;
    private String chatResponse = "respuesta";
    private List<String> streamChunks = List.of("respuesta");

    private RuntimeException listModelsError;
    private RuntimeException showModelError;
    private RuntimeException embedError;
    private RuntimeException chatError;
    private RuntimeException streamError;

    private final List<List<String>> embedBatches = new ArrayList<>();
    private String lastShowModel;
    private String lastChatModel;
    private List<OllamaMessage> lastMessages = List.of();
    private int lastNumCtx;
    private String lastFormat;
    private boolean lastStreaming;

    @Override
    public List<OllamaModel> listModels() {
        if (listModelsError != null) {
            throw listModelsError;
        }
        return models;
    }

    @Override
    public OllamaModelInfo showModel(String model) {
        lastShowModel = model;
        if (showModelError != null) {
            throw showModelError;
        }
        return new OllamaModelInfo(model, contextLength, capabilities);
    }

    @Override
    public List<float[]> embed(String model, List<String> inputs) {
        embedBatches.add(List.copyOf(inputs));
        if (embedError != null) {
            throw embedError;
        }
        return embedHandler.apply(List.copyOf(inputs));
    }

    @Override
    public String chat(String model, List<OllamaMessage> messages, int numCtx, String formatJson) {
        lastChatModel = model;
        lastMessages = List.copyOf(messages);
        lastNumCtx = numCtx;
        lastFormat = formatJson;
        lastStreaming = false;
        if (chatError != null) {
            throw chatError;
        }
        return chatResponse;
    }

    @Override
    public void chatStreaming(
            String model, List<OllamaMessage> messages, int numCtx, Consumer<String> onToken) {
        lastChatModel = model;
        lastMessages = List.copyOf(messages);
        lastNumCtx = numCtx;
        lastStreaming = true;
        if (streamError != null) {
            throw streamError;
        }
        streamChunks.forEach(onToken);
    }

    public FakeOllamaClient models(List<OllamaModel> value) {
        this.models = List.copyOf(Objects.requireNonNull(value, "value"));
        return this;
    }

    public FakeOllamaClient contextLength(OptionalInt value) {
        this.contextLength = Objects.requireNonNull(value, "value");
        return this;
    }

    public FakeOllamaClient contextLength(int value) {
        this.contextLength = OptionalInt.of(value);
        return this;
    }

    public FakeOllamaClient capabilities(List<String> value) {
        this.capabilities = List.copyOf(Objects.requireNonNull(value, "value"));
        return this;
    }

    public FakeOllamaClient embedHandler(Function<List<String>, List<float[]>> value) {
        this.embedHandler = Objects.requireNonNull(value, "value");
        return this;
    }

    public FakeOllamaClient chatResponse(String value) {
        this.chatResponse = Objects.requireNonNull(value, "value");
        return this;
    }

    public FakeOllamaClient streamChunks(List<String> value) {
        this.streamChunks = List.copyOf(Objects.requireNonNull(value, "value"));
        return this;
    }

    public FakeOllamaClient failListModels(RuntimeException error) {
        this.listModelsError = error;
        return this;
    }

    public FakeOllamaClient failShowModel(RuntimeException error) {
        this.showModelError = error;
        return this;
    }

    public FakeOllamaClient failEmbed(RuntimeException error) {
        this.embedError = error;
        return this;
    }

    public FakeOllamaClient failChat(RuntimeException error) {
        this.chatError = error;
        return this;
    }

    public FakeOllamaClient failStream(RuntimeException error) {
        this.streamError = error;
        return this;
    }

    public List<List<String>> embedBatches() {
        return List.copyOf(embedBatches);
    }

    public String lastShowModel() {
        return lastShowModel;
    }

    public String lastChatModel() {
        return lastChatModel;
    }

    public List<OllamaMessage> lastMessages() {
        return lastMessages;
    }

    public int lastNumCtx() {
        return lastNumCtx;
    }

    public String lastFormat() {
        return lastFormat;
    }

    public boolean lastStreaming() {
        return lastStreaming;
    }

    private List<float[]> defaultVectors(List<String> inputs) {
        return inputs.stream().map(text -> new float[] {1.0f, 0.0f, 0.0f}).toList();
    }
}
