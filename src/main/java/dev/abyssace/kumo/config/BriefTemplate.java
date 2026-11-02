package dev.abyssace.kumo.config;

import dev.abyssace.kumo.i18n.Messages;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Patrón: Value Object + configuración de la estrategia de extracción, no un esquema arbitrario. */
public record BriefTemplate(List<String> fields, String instructions) {
    public static final Set<String> ALLOWED_FIELDS = Set.of("documentType", "title", "summary", "keyPoints",
            "requirements", "conclusions", "tasks", "scientific", "technical");

    public BriefTemplate {
        if (fields == null || fields.stream().anyMatch(f -> f == null || !ALLOWED_FIELDS.contains(f))
                || new HashSet<>(fields).size() != fields.size()
                || !fields.containsAll(List.of("title", "summary"))) {
            throw new IllegalArgumentException(Messages.BRIEF_TEMPLATE_FIELDS);
        }
        fields = List.copyOf(fields);
        if (instructions == null || instructions.length() > 4000) {
            throw new IllegalArgumentException(Messages.BRIEF_TEMPLATE_INSTRUCTIONS);
        }
    }

    public static BriefTemplate defaults() {
        return new BriefTemplate(List.of("documentType", "title", "summary", "keyPoints", "requirements",
                "conclusions", "tasks", "scientific", "technical"),
                "Sé breve y conserva los datos esenciales. Distingue requisitos de tareas explícitamente asignadas.");
    }
}
