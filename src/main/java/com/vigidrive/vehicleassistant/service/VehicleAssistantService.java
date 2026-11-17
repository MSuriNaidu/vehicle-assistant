package com.vigidrive.vehicleassistant.service;

import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.ResponseLanguage;

public interface VehicleAssistantService {

    AdviceResponse ask(String question);

    AdviceResponse ask(String question, ResponseLanguage responseLanguage);

}
