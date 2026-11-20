package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.AdviceResponse;
public interface CoordinatorAgent {

    AdviceResponse coordinate(String message, String responseLanguage);
}
