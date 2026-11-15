package com.vigidrive.vehicleassistant.knowledge.repository;

import com.vigidrive.vehicleassistant.knowledge.DtcCode;

import java.util.Optional;

public interface DtcCodeRepository {

    Optional<DtcCode> findByCode(String code);
}
