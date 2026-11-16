package com.vigidrive.vehicleassistant.eval;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("eval")
class EvalTierConfigurationTest {

    @Test
    void requiresAnOpenRouterApiKey() {
        assertThrows(IllegalStateException.class, () -> EvalTierConfiguration.requireApiKey(Map.of()));
        assertNotNull(EvalTierConfiguration.requireApiKey(Map.of("OPENAI_API_KEY", "configured")));
    }

    @Test
    void reportsTheExpectedTierDefaults() throws Exception {
        var properties = new java.util.Properties();
        try (var input = getClass().getResourceAsStream("/application.properties")) {
            assertNotNull(input, "Evaluation test properties must be on the test classpath.");
            properties.load(input);
        }

        assertTrue(properties.getProperty("eval.delay-ms").contains("2000"));
        assertTrue(properties.getProperty("eval.runs-per-case").contains("3"));
        assertTrue(properties.getProperty("eval.max-retries").contains("3"));
    }
}
