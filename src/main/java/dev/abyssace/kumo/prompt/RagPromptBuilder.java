package dev.abyssace.kumo.prompt;

import dev.abyssace.kumo.domain.Chunk;
import dev.abyssace.kumo.domain.ScoredChunk;
import java.util.List;
import java.util.Objects;

/**
 * Patrón: Strategy concreta + Builder de prompts.
 *
 * <p>Prompt de RAG: instrucciones estrictas, contexto numerado con archivo y
 * ubicación, y la pregunta al final. Pide responder en el idioma de la pregunta
 * y citar los fragmentos usados como {@code [n]}.</p>
 */
public final class RagPromptBuilder implements PromptBuilder {

    @Override
    public String buildRagPrompt(String question, List<ScoredChunk> context) {
        Objects.requireNonNull(question, "question");
        Objects.requireNonNull(context, "context");
        if (question.isBlank()) {
            throw new IllegalArgumentException("La pregunta no puede estar vacía");
        }
        if (context.isEmpty()) {
            throw new IllegalArgumentException("El contexto no puede estar vacío");
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres kumo, un asistente de documentos que responde únicamente con el CONTEXTO proporcionado.\n")
                .append("Reglas:\n")
                .append("1. Responde en el mismo idioma en el que está escrita la PREGUNTA.\n")
                .append("2. No inventes información: si el CONTEXTO no contiene la respuesta, dilo claramente.\n")
                .append("3. Cuando uses un fragmento, cita su número entre corchetes, por ejemplo [1].\n")
                .append('\n')
                .append("CONTEXTO:\n");

        int number = 1;
        for (ScoredChunk scored : context) {
            Chunk chunk = scored.embedded().chunk();
            prompt.append('[').append(number++).append("] archivo: ")
                    .append(chunk.source().getFileName());

            String location = chunk.location().describe();
            if (!location.isBlank()) {
                prompt.append(" | ").append(location);
            }
            prompt.append('\n')
                    .append(chunk.text().strip())
                    .append("\n\n");
        }

        prompt.append("PREGUNTA:\n")
                .append(question.strip())
                .append('\n');

        return prompt.toString();
    }
}
