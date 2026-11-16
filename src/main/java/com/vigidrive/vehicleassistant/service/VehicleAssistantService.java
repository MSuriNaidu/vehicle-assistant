package com.vigidrive.vehicleassistant.service;

import com.vigidrive.vehicleassistant.model.AdviceResponse;

public interface VehicleAssistantService {

    AdviceResponse ask(String question);

}
