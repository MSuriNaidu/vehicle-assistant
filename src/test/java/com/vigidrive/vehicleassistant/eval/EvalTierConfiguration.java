package com.vigidrive.vehicleassistant.eval;

import java.util.Map;

final class EvalTierConfiguration {

    private EvalTierConfiguration() {
    }

    static String requireApiKey(Map<String, String> environment) {
        String apiKey = environment.get("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "The eval tier requires OPENAI_API_KEY because this application is configured for OpenRouter.");
        }
        return apiKey;
    }
}
