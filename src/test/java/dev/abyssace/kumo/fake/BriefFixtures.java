package dev.abyssace.kumo.fake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Patrón: Test Data Builder de fichas válidas y reproducibles. */
public final class BriefFixtures {
    private BriefFixtures() { }

    public static ObjectNode valid() {
        var mapper = new ObjectMapper();
        var value = mapper.createObjectNode();
        value.put("documentType", "document");
        value.put("title", "Proyecto kumo");
        value.put("summary", "Asistente local de documentos.");
        value.putArray("keyPoints").add("Utiliza Ollama local.");
        value.putArray("requirements").add("Debe instalarse mediante un script.");
        value.putArray("conclusions");
        value.putArray("tasks");
        value.putNull("scientific");
        value.putNull("technical");
        return value;
    }
}
