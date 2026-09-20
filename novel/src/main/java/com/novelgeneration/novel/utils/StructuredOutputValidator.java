package com.novelgeneration.novel.utils;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class StructuredOutputValidator {

    private StructuredOutputValidator() {
    }

    public static void validate(JsonNode value, JsonNode schema) {
        List<String> errors = new ArrayList<>();
        validateNode(value, schema, "$", errors);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Structured output validation failed: " + String.join("; ", errors));
        }
    }

    private static void validateNode(JsonNode value, JsonNode schema, String path, List<String> errors) {
        if (schema == null || schema.isNull()) {
            return;
        }

        String type = schema.path("type").asText("");
        if (!matchesType(value, type)) {
            errors.add(path + " must be " + type);
            return;
        }

        if ("object".equals(type)) {
            validateObject(value, schema, path, errors);
        } else if ("array".equals(type)) {
            JsonNode itemSchema = schema.get("items");
            for (int i = 0; i < value.size(); i++) {
                validateNode(value.get(i), itemSchema, path + "[" + i + "]", errors);
            }
        }
    }

    private static void validateObject(JsonNode value, JsonNode schema, String path, List<String> errors) {
        JsonNode required = schema.get("required");
        if (required != null && required.isArray()) {
            for (JsonNode field : required) {
                String name = field.asText();
                if (!value.has(name) || value.get(name).isNull()) {
                    errors.add(path + "." + name + " is required");
                }
            }
        }

        JsonNode properties = schema.get("properties");
        if (schema.path("additionalProperties").asBoolean(true) == false && properties != null) {
            Iterator<String> fields = value.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (!properties.has(field)) {
                    errors.add(path + "." + field + " is not allowed");
                }
            }
        }

        if (properties != null && properties.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> entries = properties.fields();
            while (entries.hasNext()) {
                Map.Entry<String, JsonNode> entry = entries.next();
                if (value.has(entry.getKey()) && !value.get(entry.getKey()).isNull()) {
                    validateNode(value.get(entry.getKey()), entry.getValue(), path + "." + entry.getKey(), errors);
                }
            }
        }
    }

    private static boolean matchesType(JsonNode value, String type) {
        if (value == null || value.isNull()) {
            return false;
        }
        return switch (type) {
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            case "string" -> value.isTextual();
            case "integer" -> value.isIntegralNumber();
            case "number" -> value.isNumber();
            case "boolean" -> value.isBoolean();
            default -> true;
        };
    }
}
