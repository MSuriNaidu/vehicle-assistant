package com.vigidrive.vehicleassistant.model;

public enum ResponseLanguage {
    AUTO,
    ENGLISH,
    HINDI,
    TELUGU;

    public String promptName() {
        return switch (this) {
            case AUTO -> "the same language as the user's original message";
            case ENGLISH -> "English";
            case HINDI -> "Hindi";
            case TELUGU -> "Telugu";
        };
    }
}
