package com.vigidrive.vehicleassistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public record DiagnosisResult(
        @JsonProperty(required = true) String summary,
        @JsonProperty(required = true) Urgency urgency,
        @JsonProperty(required = true) List<String> possibleCauses,
        @JsonProperty(required = true) List<String> recommendedActions,
        @JsonProperty(required = true) boolean requiresProfessionalInspection) {

    public DiagnosisResult {
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("Diagnosis summary must not be blank.");
        }
        if (urgency == null) {
            throw new IllegalArgumentException("Diagnosis urgency must not be null.");
        }
        if (possibleCauses == null || recommendedActions == null) {
            throw new IllegalArgumentException("Diagnosis cause and action lists must not be null.");
        }
        if (possibleCauses.stream().anyMatch(Objects::isNull)
                || recommendedActions.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Diagnosis lists must not contain null entries.");
        }
        summary = summary.trim();
        possibleCauses = List.copyOf(possibleCauses);
        recommendedActions = List.copyOf(recommendedActions);
    }
}
