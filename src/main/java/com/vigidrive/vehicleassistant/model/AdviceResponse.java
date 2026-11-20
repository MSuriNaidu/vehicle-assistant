package com.vigidrive.vehicleassistant.model;

public record AdviceResponse(
        AdviceUrgency urgency,
        String answer,
        boolean safetyTriggered,
        String safetyReason) {
}
