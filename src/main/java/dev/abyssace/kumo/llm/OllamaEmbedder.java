package dev.abyssace.kumo.llm;

import dev.abyssace.kumo.embed.Embedder;
import dev.abyssace.kumo.i18n.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Patrón: Adapter + Strategy.
 *
 * <p>Implementa {@link Embedder} sobre {@link OllamaClient}. Envía los textos
 * por lotes para no abrir una petición por fragmento y comprueba que todos los
 * vectores tengan la misma dimensión.</p>
 */
public final class OllamaEmbedder implements Embedder {

    /** Tamaño de lote por defecto; se puede bajar en pruebas. */
    public static final int DEFAULT_BATCH_SIZE = 16;

    private final OllamaClient client;
    private final String model;
    private final int batchSize;

    public OllamaEmbedder(OllamaClient client, String model) {
        this(client, model, DEFAULT_BATCH_SIZE);
    }

    public OllamaEmbedder(OllamaClient client, String model, int batchSize) {
        this.client = Objects.requireNonNull(client, "client");
        this.model = requireText(model, "model");
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize debe ser >= 1, pero fue " + batchSize);
        }
        this.batchSize = batchSize;
    }

    @Override
    public float[] embed(String text) {
        Objects.requireNonNull(text, "text");
        List<float[]> vectors = embedAll(List.of(text));
        return vectors.get(0);
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        Objects.requireNonNull(texts, "texts");
        if (texts.isEmpty()) {
            return List.of();
        }
        texts.forEach(text -> Objects.requireNonNull(text, "text"));

        List<float[]> result = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += batchSize) {
            int end = Math.min(start + batchSize, texts.size());
            List<String> batch = texts.subList(start, end);
            List<float[]> vectors = client.embed(model, batch);
            if (vectors == null || vectors.size() != batch.size()) {
                throw new OllamaException(
                        Messages.OLLAMA_EMPTY_EMBEDDINGS.formatted(model, batch.size()));
            }
            for (float[] vector : vectors) {
                if (vector == null || vector.length == 0) {
                    throw new OllamaException(
                            Messages.OLLAMA_EMPTY_EMBEDDINGS.formatted(model, batch.size()));
                }
                if (!result.isEmpty() && vector.length != result.get(0).length) {
                    throw new OllamaException(Messages.OLLAMA_EMBEDDING_DIMENSIONS.formatted(
                            model, result.get(0).length, vector.length));
                }
                result.add(vector.clone());
            }
        }
        return List.copyOf(result);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " no puede estar vacío");
        }
        return trimmed;
    }
}
