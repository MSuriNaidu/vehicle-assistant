package com.vigidrive.vehicleassistant.model;

import java.util.List;

public record VehicleIssue(String make, String model, Integer year,
                           List<String> symptoms, List<String> dtcCodes) {
}