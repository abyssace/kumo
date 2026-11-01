package dev.abyssace.kumo.rag;

import dev.abyssace.kumo.domain.Answer;
import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.Citation;
import dev.abyssace.kumo.domain.EmbeddedChunk;
import dev.abyssace.kumo.domain.ScoredChunk;
import dev.abyssace.kumo.embed.Embedder;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.llm.LlmClient;
import dev.abyssace.kumo.prompt.PromptBuilder;
import dev.abyssace.kumo.store.VectorStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Patrón: Facade + inyección de dependencias por constructor.
 *
 * <p>Orquesta el flujo RAG completo: indexar fragmentos, recuperar los más
 * similares y pedirle al modelo una respuesta con citas. No sabe si el store es
 * memoria o disco, ni si el modelo es Ollama o un doble de pruebas.</p>
 */
public final class RagService {

    private static final int EXCERPT_CHARS = 240;

    private final Embedder embedder;
    private final VectorStore store;
    private final LlmClient llm;
    private final PromptBuilder promptBuilder;
    private final int topK;
    private final double minScore;

    public RagService(
            Embedder embedder,
            VectorStore store,
            LlmClient llm,
            PromptBuilder promptBuilder,
            int topK,
            double minScore) {
        this.embedder = Objects.requireNonNull(embedder, "embedder");
        this.store = Objects.requireNonNull(store, "store");
        this.llm = Objects.requireNonNull(llm, "llm");
        this.promptBuilder = Objects.requireNonNull(promptBuilder, "promptBuilder");
        if (topK < 1) {
            throw new IllegalArgumentException("topK debe ser >= 1, pero fue " + topK);
        }
        if (minScore < -1.0 || minScore > 1.0) {
            throw new IllegalArgumentException("minScore debe estar entre -1 y 1, pero fue " + minScore);
        }
        this.topK = topK;
        this.minScore = minScore;
    }

    /**
     * Embebe e indexa los fragmentos indicados. Devuelve cuántos se procesaron.
     */
    public int index(List<Chunk> chunks) {
        Objects.requireNonNull(chunks, "chunks");
        if (chunks.isEmpty()) {
            return 0;
        }

        List<String> texts = chunks.stream().map(Chunk::text).toList();
        List<float[]> vectors = embedder.embedAll(texts);
        Objects.requireNonNull(vectors, "vectors");
        if (vectors.size() != chunks.size()) {
            throw new IllegalStateException(
                    "El embedder devolvió " + vectors.size() + " vectores para " + chunks.size() + " fragmentos");
        }

        List<EmbeddedChunk> embedded = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            embedded.add(new EmbeddedChunk(chunks.get(i), vectors.get(i)));
        }
        store.add(embedded);
        return embedded.size();
    }

    /**
     * Recupera los fragmentos más relevantes para la pregunta, aplicando el
     * umbral opcional {@code minScore}.
     */
    public List<ScoredChunk> retrieve(String question) {
        Objects.requireNonNull(question, "question");
        if (question.isBlank()) {
            throw new IllegalArgumentException("La pregunta no puede estar vacía");
        }

        float[] query = embedder.embed(question);
        return store.search(query, topK).stream()
                .filter(scored -> scored.score() >= minScore)
                .toList();
    }

    /** Responde la pregunta con RAG y devuelve el texto junto a sus citas. */
    public Answer ask(String question) {
        List<ScoredChunk> context = retrieve(question);
        if (context.isEmpty()) {
            return new Answer(question, Messages.NO_CONTEXT, List.of());
        }

        String prompt = promptBuilder.buildRagPrompt(question, context);
        String text = llm.complete(prompt);
        return new Answer(question, text, citations(context));
    }

    /**
     * Variante en streaming: reenvía cada token a {@code onToken} mientras
     * acumula la respuesta completa.
     */
    public Answer askStreaming(String question, Consumer<String> onToken) {
        Objects.requireNonNull(onToken, "onToken");

        List<ScoredChunk> context = retrieve(question);
        if (context.isEmpty()) {
            onToken.accept(Messages.NO_CONTEXT);
            return new Answer(question, Messages.NO_CONTEXT, List.of());
        }

        String prompt = promptBuilder.buildRagPrompt(question, context);
        StringBuilder text = new StringBuilder();
        llm.stream(prompt, token -> {
            text.append(token);
            onToken.accept(token);
        });
        return new Answer(question, text.toString(), citations(context));
    }

    private static List<Citation> citations(List<ScoredChunk> context) {
        return context.stream()
                .map(scored -> Citation.from(scored, EXCERPT_CHARS))
                .toList();
    }
}
