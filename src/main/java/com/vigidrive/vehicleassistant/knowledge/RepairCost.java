package com.vigidrive.vehicleassistant.knowledge;

public record RepairCost(String repair, VehicleSegment segment, int minInr, int maxInr,
                         String source, String asOf) {
}
