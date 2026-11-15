package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.VehicleIssue;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface IntakeAgent {

    @SystemMessage("""
        You extract vehicle problem details from a car owner's message.
        Rules:
        - Extract only what the user actually said. If make, model, year or any other
          field is not mentioned, leave it null. Never guess.
        - Copy fault codes exactly as written (for example P0300).
        - List each distinct symptom as a separate short English phrase, even if the
          message is in Telugu, Hindi or mixed languages.
        - Do not diagnose or give advice.
        """)
    @UserMessage("User message: {{message}}")
    @Agent(
            name = "Vehicle Intake Agent",
            description = "Extracts vehicle details, symptoms and fault codes from a user's message",
            outputKey = "vehicleIssue"
    )
    VehicleIssue intakeVehicleIssue(@V("message") String message);

}
