package dev.abyssace.kumo.prompt;

import dev.abyssace.kumo.domain.ScoredChunk;
import java.util.List;

/**
 * Patrón: Strategy (contrato) + Builder de prompts.
 *
 * <p>Construye el texto que recibe el modelo. Mantenerlo aparte permite probar
 * el prompt sin red y cambiarlo sin tocar la lógica RAG.</p>
 */
public interface PromptBuilder {

    String buildRagPrompt(String question, List<ScoredChunk> context);
}
