package com.vigidrive.vehicleassistant.model;

public record AdviceResponse(
        Urgency urgency,
        String answer,
        boolean safetyTriggered,
        String safetyReason) {
}
