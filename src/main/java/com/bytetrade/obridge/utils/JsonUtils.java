package com.bytetrade.obridge.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String convertToSnakeCase(String camelCaseJson) throws JsonProcessingException {
        if (camelCaseJson == null || camelCaseJson.trim().isEmpty()) {
            return camelCaseJson;
        }
        JsonNode jsonNode = mapper.readTree(camelCaseJson);
        JsonNode transformed = transformNode(jsonNode);
        return mapper.writeValueAsString(transformed);
    }

    private static JsonNode transformNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }

        if (node.isObject()) {
            ObjectNode objectNode = mapper.createObjectNode();
            node.fields().forEachRemaining(entry -> {
                String key = camelToSnake(entry.getKey());
                JsonNode value = transformNode(entry.getValue());
                objectNode.set(key, value);
            });
            return objectNode;
        }

        if (node.isArray()) {
            ArrayNode arrayNode = mapper.createArrayNode();
            node.elements().forEachRemaining(element -> arrayNode.add(transformNode(element)));
            return arrayNode;
        }

        // Return all primitive types as is
        return node;
    }

    private static String camelToSnake(String str) {
        StringBuilder result = new StringBuilder();
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static String toCompactJsonString(Object object) {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES) // Convert camelCase to snake_case
                .create();

        String json = gson.toJson(object);

        JsonElement jsonElement = JsonParser.parseString(json);
        return gson.toJson(jsonElement);
    }

    public static <T> T parseObject(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON parsing error", e);
        }
    }

    public static String toJsonString(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization error", e);
        }
    }
}