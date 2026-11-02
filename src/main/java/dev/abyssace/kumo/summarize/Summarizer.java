package dev.abyssace.kumo.summarize;

import dev.abyssace.kumo.chunk.Chunker;
import dev.abyssace.kumo.domain.Document;

/** Patrón: Strategy; independiente del transporte de LLM y de la interfaz de usuario. */
public interface Summarizer {
    String summarize(Document document, Chunker chunker, SummarySession session);
}
