package dev.abyssace.kumo.llm;

/** Patrón: extensión de Port; conserva intacto el contrato central LlmClient. */
public interface StructuredLlmClient extends LlmClient {
    String completeStructured(String prompt, String jsonSchema);
}
