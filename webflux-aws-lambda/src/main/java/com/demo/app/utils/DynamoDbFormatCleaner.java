package com.demo.app.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamoDbFormatCleaner {

    private final ObjectMapper mapper;

    // ============================
    //  NORMALIZADOR PRINCIPAL
    // ============================
    public Map<String, Object> normalize(String rawLambdaJson) {
        try {
            Map<String, Object> response = mapper.readValue(rawLambdaJson, Map.class);

            String dataStr = (String) response.get("data");
            Map<String, Object> dataJson = mapper.readValue(dataStr, Map.class);

            // Normalizamos el campo "value"
            Object cleanedValue = cleanValue(dataJson.get("value"));
            dataJson.put("value", cleanedValue);

            response.put("data", dataJson);
            return response;

        } catch (Exception ex) {
            log.error("Error normalizando DynamoDB", ex);
            throw new RuntimeException("Error normalizando el JSON DynamoDB", ex);
        }
    }

    // Detecta si un string ya contiene JSON válido
    private boolean isJsonArray(String value) {
        return value.trim().startsWith("[") && value.trim().endsWith("]");
    }

    private boolean isJsonObject(String value) {
        return value.trim().startsWith("{") && value.trim().endsWith("}");
    }

    // ======================================================
    //      LIMPIADOR RECURSIVO (Soporta JSON corrupto)
    // ======================================================
    private Object cleanValue(Object value) {

        // Ya es MAP → limpiar recursivamente
        if (value instanceof Map<?, ?> mapVal) {
            Map<String, Object> cleaned = new LinkedHashMap<>();
            mapVal.forEach((k, v) -> cleaned.put(k.toString(), cleanValue(v)));
            return cleaned;
        }

        // Ya es LIST → limpiar recursivamente
        if (value instanceof List<?> listVal) {
            List<Object> cleaned = new ArrayList<>();
            listVal.forEach(v -> cleaned.add(cleanValue(v)));
            return cleaned;
        }

        // No es string → devolver directo
        if (!(value instanceof String raw)) {
            return value;
        }

        raw = raw.trim();

        // DETECTAR FORMATO NUEVO → STRING con JSON adentro
        if (isJsonArray(raw) || isJsonObject(raw)) {
            try {
                // lo parseamos con Jackson
                Object parsed = mapper.readValue(raw, Object.class);
                return cleanValue(parsed); // limpiar recursivamente
            } catch (Exception ex) {
                // si falla la deserialización, continuar con AttributeValue parser
                log.warn("String parece JSON pero no se pudo parsear: {}", raw);
            }
        }

        // Si no contiene AttributeValue → retornar intacto
        if (!raw.contains("AttributeValue")) {
            return raw;
        }

        // Modo DynamoDB → parsear con el parser avanzado
        return cleanAttributeValueString(raw);
    }


    // ======================================================
    //       EXTRACCIÓN DE BLOQUES AttributeValue(...)
    // ======================================================
    private List<String> extractAttributeValueBlocks(String str) {
        List<String> blocks = new ArrayList<>();

        // Regex tolerante para captar bloques aunque estén rotos
        Pattern pattern = Pattern.compile("AttributeValue\\(([^()]*(\\([^)]*\\))?)*\\)");
        Matcher matcher = pattern.matcher(str);

        while (matcher.find()) {
            blocks.add(matcher.group());
        }

        return blocks;
    }

    // ======================================================
    //     REPARADOR PARA STRINGS DYNAMODB ROTOS
    // ======================================================
    private Object repairCorruptedString(String raw) {
        List<String> fragments = new ArrayList<>();

        Matcher m = Pattern.compile("AttributeValue\\([^)]*\\)").matcher(raw);
        while (m.find()) fragments.add(m.group());

        // Si no hay nada → return string literal
        if (fragments.isEmpty()) {
            return raw;
        }

        // Convertimos cada fragmento encontrado
        List<Object> parsedList = new ArrayList<>();
        for (String f : fragments) {
            parsedList.add(parseSingleBlock(f));
        }

        return parsedList;
    }

    // ======================================================
    //  PARSEAR UN BLOQUE AttributeValue(...)
    // ======================================================
    private Object parseSingleBlock(String block) {

        block = block.trim();

        if (block.startsWith("AttributeValue(BOOL=")) {
            return Boolean.parseBoolean(
                    block.replace("AttributeValue(BOOL=", "")
                            .replace(")", "")
            );
        }

        if (block.startsWith("AttributeValue(S=")) {
            return block.replace("AttributeValue(S=", "")
                    .replace(")", "")
                    .trim();
        }

        if (block.startsWith("AttributeValue(N=")) {
            String num = block.replace("AttributeValue(N=", "")
                    .replace(")", "")
                    .trim();
            try { return Integer.parseInt(num); }
            catch (Exception e) { return num; }
        }

        if (block.startsWith("AttributeValue(M=")) {
            String inner = extractBody(block, "AttributeValue(M=");
            return cleanValue(parseRawMap(inner));
        }

        if (block.startsWith("AttributeValue(L=[")) {
            String inner = extractBody(block, "AttributeValue(L=[");
            return cleanValue(parseRawList(inner));
        }

        return block; // fallback
    }

    // ======================================================
    //     EXTRACCIÓN DE CONTENIDO BRUTO DE MAP/LIST
    // ======================================================
    private String extractBody(String raw, String prefix) {
        String s = raw.substring(prefix.length());

        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("})")) s = s.substring(0, s.length() - 2);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);
        if (s.endsWith("]")) s = s.substring(0, s.length() - 1);

        return s.trim();
    }

    // ======================================================
    //      PARSEADOR TOLERANTE DE MAP
    // ======================================================
    private Map<String, Object> parseRawMap(String raw) {
        Map<String, Object> map = new LinkedHashMap<>();

        String[] parts = raw.split("(?<=}),");

        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), cleanValue(kv[1].trim()));
            }
        }

        return map;
    }

    // ======================================================
    //      PARSEADOR TOLERANTE DE LIST
    // ======================================================
    private List<Object> parseRawList(String raw) {
        List<Object> list = new ArrayList<>();

        String[] parts = raw.split("(?<=}),");

        for (String p : parts) {
            list.add(cleanValue(p.trim()));
        }

        return list;
    }

    // ======================================================
    //    LIMPIADOR PRINCIPAL DE STRINGS AttributeValue(...)
    // ======================================================
    private Object cleanAttributeValueString(String raw) {

        List<String> blocks = extractAttributeValueBlocks(raw);

        if (blocks.isEmpty()) {
            return repairCorruptedString(raw);
        }

        List<Object> parsed = new ArrayList<>();
        for (String block : blocks) {
            parsed.add(parseSingleBlock(block));
        }

        return parsed;
    }
}