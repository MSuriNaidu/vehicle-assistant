package com.vigidrive.vehicleassistant.safety;

public record SafetyResult(boolean dangerous, String reason, String message) {

    public static SafetyResult safe() {
        return new SafetyResult(false, null, null);
    }
}
