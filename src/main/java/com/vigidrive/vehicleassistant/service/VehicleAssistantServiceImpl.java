package com.vigidrive.vehicleassistant.service;

import com.vigidrive.vehicleassistant.tools.VehicleAssistantAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VehicleAssistantServiceImpl implements VehicleAssistantService {

    private final VehicleAssistantAiService vehicleAssistantAiService;

    @Override
    public String ask(String question) {
        return vehicleAssistantAiService.chat(question);
    }
}
