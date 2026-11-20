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
        - Extract only facts explicitly stated by the user. Never infer or guess.
        - If make, model, or year is not explicitly stated, use null.
        - Always use arrays for symptoms, dtcCodes and safetyConcerns; use an empty
          array when none are explicitly stated.
        - List symptoms and safety concerns as distinct, short English phrases,
          translating their meaning from Telugu, Hindi or mixed-language messages
          without adding any facts.
        - Copy every diagnostic trouble code exactly as written, including case.
        - Safety concerns are only reported observations such as smoke, fire,
          fuel smell/leak, failed brakes/steering, severe overheating, or critical
          warning lights. Report them; do not assess their danger.
        - Do not diagnose, recommend repairs, estimate cost, or decide whether the
          vehicle is safe to drive.
        - Return only the VehicleIssue fields. Use null for missing scalar values.
        """)
    @UserMessage("User message: {{message}}")
    @Agent(
            name = "Vehicle Intake Agent",
            description = "Extracts only stated vehicle details, symptoms, DTC codes and safety observations",
            outputKey = "vehicleIssue"
    )
    VehicleIssue intakeVehicleIssue(@V("message") String message);

}
