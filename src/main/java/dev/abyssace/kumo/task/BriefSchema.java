package dev.abyssace.kumo.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.abyssace.kumo.config.BriefTemplate;
import dev.abyssace.kumo.i18n.Messages;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Patrón: Builder + Validator del subconjunto fijo de JSON Schema generado por kumo. */
public final class BriefSchema {
    private final ObjectMapper mapper = new ObjectMapper();

    public ObjectNode create(BriefTemplate template, boolean sourceCode) {
        Map<String, JsonNode> fields = new LinkedHashMap<>();
        for (String field : template.fields()) {
            fields.put(field, switch (field) {
                case "title", "summary" -> string(false);
                case "documentType" -> mapper.createObjectNode().put("type", "string")
                        .set("enum", mapper.valueToTree(sourceCode ? new String[] {"code"}
                                : new String[] {"notes", "scientific", "document"}));
                case "keyPoints", "requirements", "conclusions" -> array(string(false));
                case "tasks" -> array(object(Map.of("description", string(false), "assignee", string(true),
                        "dueDate", string(true), "evidence", string(false)), false));
                case "scientific" -> object(Map.of("objective", string(true), "method", string(true),
                        "results", array(string(false)), "limitations", array(string(false))), true);
                case "technical" -> object(Map.of("purpose", string(true), "components", array(string(false)),
                        "flow", array(string(false)), "dependencies", array(string(false))), true);
                default -> throw new IllegalArgumentException(Messages.BRIEF_TEMPLATE_FIELDS);
            });
        }
        ObjectNode schema = object(fields, false);
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        return schema;
    }

    private ObjectNode string(boolean nullable) {
        ObjectNode schema = mapper.createObjectNode();
        if (nullable) {
            schema.set("type", mapper.valueToTree(new String[] {"string", "null"}));
        } else {
            schema.put("type", "string");
        }
        schema.put("minLength", 1);
        return schema;
    }

    private ObjectNode array(JsonNode items) {
        return mapper.createObjectNode().put("type", "array").set("items", items);
    }

    private ObjectNode object(Map<String, JsonNode> properties, boolean nullable) {
        ObjectNode result = mapper.createObjectNode();
        if (nullable) {
            result.set("type", mapper.valueToTree(new String[] {"object", "null"}));
        } else {
            result.put("type", "object");
        }
        result.set("properties", mapper.valueToTree(properties));
        result.set("required", mapper.valueToTree(properties.keySet()));
        result.put("additionalProperties", false);
        return result;
    }

    public void validate(JsonNode value, JsonNode schema) {
        validate(value, schema, "$");
    }

    private void validate(JsonNode value, JsonNode schema, String path) {
        if (value == null || !matches(value, schema.get("type"))) {
            fail(path);
        }
        if (value.isNull()) {
            return;
        }
        if (schema.has("enum")) {
            boolean found = false;
            for (JsonNode item : schema.get("enum")) {
                found |= item.equals(value);
            }
            if (!found) {
                fail(path);
            }
        }
        if (value.isTextual() && value.asText().isBlank()) {
            fail(path);
        }
        if (value.isArray()) {
            for (int i = 0; i < value.size(); i++) {
                validate(value.get(i), schema.get("items"), path + "[" + i + "]");
            }
        }
        if (value.isObject()) {
            JsonNode properties = schema.get("properties");
            for (JsonNode key : schema.get("required")) {
                if (!value.has(key.asText())) {
                    fail(path + "." + key.asText());
                }
            }
            Iterator<String> keys = value.fieldNames();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!properties.has(key)) {
                    fail(path + "." + key);
                }
                validate(value.get(key), properties.get(key), path + "." + key);
            }
        }
    }

    private boolean matches(JsonNode value, JsonNode type) {
        if (type.isArray()) {
            for (JsonNode candidate : type) {
                if (matches(value, candidate)) {
                    return true;
                }
            }
            return false;
        }
        return switch (type.asText()) {
            case "string" -> value.isTextual();
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            case "null" -> value.isNull();
            default -> false;
        };
    }

    private void fail(String path) {
        throw new TaskException(Messages.BRIEF_SCHEMA_ERROR.formatted(path));
    }
}
