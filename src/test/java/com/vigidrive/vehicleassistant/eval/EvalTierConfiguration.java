package com.vigidrive.vehicleassistant.eval;

import java.util.Map;

final class EvalTierConfiguration {

    private EvalTierConfiguration() {
    }

    static String requireApiKey(Map<String, String> environment) {
        String apiKey = environment.get("GROQ_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "The eval tier requires GROQ_API_KEY for the configured OpenAI-compatible provider.");
        }
        return apiKey;
    }
}
