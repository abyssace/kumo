package dev.abyssace.kumo.fake;

import dev.abyssace.kumo.llm.StructuredLlmClient;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Patrón: Test Double con registro de peticiones estructuradas y resúmenes intermedios. */
public final class FakeStructuredLlmClient implements StructuredLlmClient {
    public String response = BriefFixtures.valid().toString();
    public String summary = "Síntesis breve de la fuente.";
    public final List<String> prompts = new ArrayList<>();
    public final List<String> schemas = new ArrayList<>();
    public int summaries;

    @Override
    public String completeStructured(String prompt, String jsonSchema) {
        prompts.add(prompt);
        schemas.add(jsonSchema);
        return response;
    }

    @Override
    public String complete(String prompt) {
        summaries++;
        return summary;
    }

    @Override
    public void stream(String prompt, Consumer<String> onToken) {
        throw new AssertionError("brief y write no deben transmitir tokens");
    }
}
