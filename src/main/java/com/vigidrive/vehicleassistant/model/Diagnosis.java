package com.vigidrive.vehicleassistant.model;

import java.util.List;

public record Diagnosis(String urgency, List<Cause> causes) {
}