package com.vigidrive.vehicleassistant.service;

import com.vigidrive.vehicleassistant.model.VehicleIssue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VehicleIssueValidatorTest {

    private final VehicleIssueValidator validator = new VehicleIssueValidator();

    @Test
    void retainsOnlyStatedVehicleScalarsAndExactDtcText() {
        VehicleIssue extracted = new VehicleIssue("Honda", "City", 2021,
                List.of("engine misfire", "  ", "engine misfire"),
                List.of("P0300", "p0420", "P0400"),
                List.of("fuel smell"));

        VehicleIssue validated = validator.validate(extracted,
                "My Honda City shows p0420 and has an engine misfire.");

        assertEquals("Honda", validated.make());
        assertEquals("City", validated.model());
        assertEquals(null, validated.year());
        assertEquals(List.of("engine misfire"), validated.symptoms());
        assertEquals(List.of("p0420"), validated.dtcCodes());
        assertEquals(List.of("fuel smell"), validated.safetyConcerns());
    }

    @Test
    void normalizesNullListsToEmptyImmutableLists() {
        VehicleIssue extracted = new VehicleIssue(null, null, null, null, null, null);

        VehicleIssue validated = validator.validate(extracted, "My car is noisy");

        assertEquals(List.of(), validated.symptoms());
        assertEquals(List.of(), validated.dtcCodes());
        assertEquals(List.of(), validated.safetyConcerns());
    }
}
