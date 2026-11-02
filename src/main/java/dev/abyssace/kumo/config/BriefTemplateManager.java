package dev.abyssace.kumo.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.abyssace.kumo.i18n.Messages;
import dev.abyssace.kumo.task.TaskException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/** Patrón: Repository de plantillas. Crea la predeterminada sin reemplazar ediciones del usuario. */
public final class BriefTemplateManager {
    public static final String FILE_NAME = "brief-template.json";
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private final Path home;

    public BriefTemplateManager(Path home) {
        this.home = Objects.requireNonNull(home, "home");
    }

    public Path defaultPath() {
        return home.resolve(FILE_NAME).toAbsolutePath().normalize();
    }

    public BriefTemplate load(Path custom) {
        Path path = custom == null ? defaultPath() : custom.toAbsolutePath().normalize();
        try {
            if (custom == null) {
                Files.createDirectories(path.getParent());
                try {
                    Files.writeString(path, mapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(BriefTemplate.defaults()), StandardOpenOption.CREATE_NEW);
                } catch (FileAlreadyExistsException ignored) {
                    // Nunca se reemplaza una plantilla existente.
                }
            } else if (!Files.isRegularFile(path)) {
                throw new TaskException(Messages.BRIEF_TEMPLATE_MISSING.formatted(path));
            }
            var json = mapper.readTree(Files.readString(path));
            if (json == null || !json.isObject() || json.size() != 2 || !json.has("fields")
                    || !json.has("instructions") || !json.get("fields").isArray()
                    || !json.get("instructions").isTextual()) {
                throw new IllegalArgumentException(Messages.BRIEF_TEMPLATE_PROPERTIES);
            }
            for (var field : json.get("fields")) {
                if (!field.isTextual()) {
                    throw new IllegalArgumentException(Messages.BRIEF_TEMPLATE_FIELDS);
                }
            }
            return mapper.treeToValue(json, BriefTemplate.class);
        } catch (IOException | IllegalArgumentException ex) {
            throw new TaskException(Messages.BRIEF_TEMPLATE_INVALID.formatted(path, ex.getMessage()));
        }
    }
}
