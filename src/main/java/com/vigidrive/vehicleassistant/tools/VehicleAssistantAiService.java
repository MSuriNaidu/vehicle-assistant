package com.vigidrive.vehicleassistant.tools;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface VehicleAssistantAiService {

    @SystemMessage("You are a helpful AI assistant.")
    String chat(String message);
}
