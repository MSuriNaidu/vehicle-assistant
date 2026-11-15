package com.vigidrive.vehicleassistant.knowledge.repository;

import com.vigidrive.vehicleassistant.knowledge.ModelInfo;

import java.util.Optional;

public interface ModelInfoRepository {

    Optional<ModelInfo> findByModel(String model);
}
