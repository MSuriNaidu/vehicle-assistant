package com.vigidrive.vehicleassistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Diagnosis(@JsonProperty(required = true) Urgency urgency, List<Cause> causes) {
}