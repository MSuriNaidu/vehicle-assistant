package com.vigidrive.vehicleassistant.safety;

import com.vigidrive.vehicleassistant.model.VehicleIssue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafetyServiceTest {

    private final SafetyService safetyService = new SafetyService(new SafetyGuard());

    @Test
    void detectsUrgentHazardsFromOriginalMessageOrIntakeObservations() {
        List<String> hazards = List.of(
                "Smoke is coming from the engine",
                "There is a fuel leak",
                "The brakes failed",
                "I lost control of the car",
                "The engine is severely overheating",
                "The airbag warning light is on",
                "The red warning light is on");

        for (String hazard : hazards) {
            assertTrue(safetyService.evaluate(hazard, null).dangerous(), hazard);
        }

        VehicleIssue extractedConcern = new VehicleIssue(null, null, null, List.of(),
                List.of(), List.of("loss of braking"));
        assertTrue(safetyService.evaluate("The car is acting strangely", extractedConcern).dangerous());
    }

    @Test
    void detectsConfiguredHighRiskCodesAndAllowsOrdinaryCodes() {
        SafetyGuard configuredGuard = new SafetyGuard();
        configuredGuard.setDangerousDtcCodes("P0217, P0300");
        SafetyService configuredService = new SafetyService(configuredGuard);

        assertEquals("DTC_P0300",
                configuredService.evaluate("Scanner reports P0300", null).reason());
        assertEquals(false,
                configuredService.evaluate("Scanner reports P0420", null).dangerous());
    }
}
