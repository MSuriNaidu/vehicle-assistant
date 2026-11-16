package com.vigidrive.vehicleassistant.eval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vigidrive.vehicleassistant.model.Urgency;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EvalCaseLoader {

    private static final String RESOURCE = "/eval-cases.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern CASE_ID_PATTERN =
            Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

    private EvalCaseLoader() {
    }

    public static List<EvalCase> load() {
        try (InputStream input = EvalCaseLoader.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Evaluation resource " + RESOURCE + " is missing (case id unavailable).");
            }
            return load(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read evaluation resource " + RESOURCE
                    + " (case id unavailable).", exception);
        }
    }

    static List<EvalCase> load(String json) {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Malformed evaluation JSON near case "
                    + caseIdAt(json, exception) + ".", exception);
        }

        if (root == null || !root.isArray()) {
            throw new IllegalArgumentException("Malformed evaluation data near case <unknown>: expected a JSON array.");
        }

        List<EvalCase> cases = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (JsonNode node : root) {
            String caseId = node.path("id").asText("<missing id>");
            if (!node.isObject()) {
                throw new IllegalArgumentException("Malformed evaluation case " + caseId + ": expected an object.");
            }
            if (caseId.equals("<missing id>") || caseId.isBlank()) {
                throw new IllegalArgumentException("Malformed evaluation case <missing id>: id is required.");
            }
            if (!ids.add(caseId)) {
                throw new IllegalArgumentException("Duplicate evaluation case id: " + caseId + ".");
            }

            validateUrgencies(node, caseId);
            try {
                cases.add(OBJECT_MAPPER.treeToValue(node, EvalCase.class));
            } catch (JsonProcessingException | IllegalArgumentException exception) {
                throw new IllegalArgumentException("Malformed evaluation case " + caseId + ".", exception);
            }
        }
        return List.copyOf(cases);
    }

    private static void validateUrgencies(JsonNode node, String caseId) {
        JsonNode urgencies = node.get("acceptableUrgency");
        if (urgencies == null || urgencies.isNull()) {
            return;
        }
        if (!urgencies.isArray()) {
            throw new IllegalArgumentException("Malformed acceptableUrgency for case " + caseId
                    + ": expected an array of Urgency values.");
        }
        for (JsonNode urgency : urgencies) {
            String value = urgency.asText();
            try {
                Urgency.valueOf(value);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Unknown urgency '" + value + "' in evaluation case "
                        + caseId + ".", exception);
            }
        }
    }

    private static String caseIdAt(String json, JsonProcessingException exception) {
        if (json == null) {
            return "<unknown>";
        }
        long line = exception.getLocation() == null ? 1 : exception.getLocation().getLineNr();
        long column = exception.getLocation() == null ? 1 : exception.getLocation().getColumnNr();
        int offset = 0;
        for (long currentLine = 1; currentLine < line && offset < json.length(); currentLine++) {
            int newline = json.indexOf('\n', offset);
            offset = newline < 0 ? json.length() : newline + 1;
        }
        offset = Math.min(json.length(), offset + (int) Math.max(0, column - 1));
        Matcher matcher = CASE_ID_PATTERN.matcher(json.substring(0, offset));
        String caseId = "<unknown>";
        while (matcher.find()) {
            caseId = matcher.group(1);
        }
        return caseId;
    }
}
