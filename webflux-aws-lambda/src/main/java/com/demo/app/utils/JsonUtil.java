package com.demo.app.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON", e);
        }
    }

    public static Map<String, Object> fromJson(String response, TypeReference<Object> typeReference) {
        try {
            return (Map<String, Object>) MAPPER.readValue(response, typeReference);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON", e);
        }
    }
}

