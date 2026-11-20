package com.vigidrive.vehicleassistant.model;

import java.util.List;
import java.util.Objects;

public record VehicleIssue(String make, String model, Integer year,
                           List<String> symptoms, List<String> dtcCodes,
                           List<String> safetyConcerns) {

    public VehicleIssue {
        symptoms = cleanList(symptoms);
        dtcCodes = cleanList(dtcCodes);
        safetyConcerns = cleanList(safetyConcerns);
    }

    public VehicleIssue(String make, String model, Integer year,
                        List<String> symptoms, List<String> dtcCodes) {
        this(make, model, year, symptoms, dtcCodes, List.of());
    }

    private static List<String> cleanList(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }
}