package com.vigidrive.vehicleassistant.model;

import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;

public enum Urgency {
    STOP_NOW,
    DRIVE_CAREFULLY,
    SERVICE_SOON,
    ROUTINE;

    public static Urgency fromModelValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new InvalidModelOutputException(
                    "Unsupported diagnosis urgency '" + value
                            + "'. Expected one of: STOP_NOW, DRIVE_CAREFULLY, SERVICE_SOON, ROUTINE.",
                    exception);
        }
    }
}
