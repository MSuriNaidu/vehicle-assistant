package com.vigidrive.vehicleassistant.knowledge.repository;

import com.vigidrive.vehicleassistant.knowledge.RepairCost;
import com.vigidrive.vehicleassistant.knowledge.VehicleSegment;

import java.util.Optional;

public interface RepairCostRepository {

    Optional<RepairCost> findByRepairAndSegment(String repair, VehicleSegment segment);
}
