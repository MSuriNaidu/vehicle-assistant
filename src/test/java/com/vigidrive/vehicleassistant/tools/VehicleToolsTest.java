package com.vigidrive.vehicleassistant.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vigidrive.vehicleassistant.knowledge.repository.DtcCodeRepository;
import com.vigidrive.vehicleassistant.knowledge.repository.json.JsonModelInfoRepository;
import com.vigidrive.vehicleassistant.knowledge.repository.json.JsonRepairCostRepository;
import com.vigidrive.vehicleassistant.safety.SafetyGuard;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class VehicleToolsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
    private final VehicleTools vehicleTools = new VehicleTools(
            mock(DtcCodeRepository.class),
            new JsonModelInfoRepository(resourceLoader, objectMapper),
            new JsonRepairCostRepository(resourceLoader, objectMapper),
            new SafetyGuard());

    @Test
    void estimatesBrakePadCostForHyundaiI20UsingPlainRepairAndFullMakeModel() {
        String estimate = vehicleTools.estimateCost("brake pads", "Hyundai i20");

        assertTrue(estimate.contains("on Hyundai i20"), estimate);
        assertFalse(estimate.contains("Hyundai Hyundai"), estimate);
        assertTrue(estimate.contains("INR 950-2450"), estimate);
        assertTrue(estimate.contains("2026-11"), estimate);
    }

    @Test
    void estimatesBrakePadCostForMarutiSwiftIncludingYearAndMake() {
        String estimate = vehicleTools.estimateCost(
                "Brake pad replacement", "2019 Maruti Suzuki Swift");

        assertTrue(estimate.contains("on Maruti Suzuki Swift"), estimate);
        assertTrue(estimate.contains("INR 950-2450"), estimate);
    }

    @Test
    void doesNotUseBrakePadEstimateWhenMakeConflictsWithKnownModel() {
        String estimate = vehicleTools.estimateCost("brake pads", "Toyota Swift");

        assertTrue(estimate.contains("No model or segment reference data"), estimate);
    }
}
