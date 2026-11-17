package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.Diagnosis;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DiagnosisAgent {

    @SystemMessage("""
        You are an automotive diagnostician for passenger cars in India.
        Work only from the vehicle issue you are given.
        Rules:
        - Rank likely causes from most to least probable. Probabilities are rough
          estimates and should add up to about 100.
        - Return at most 4 causes. Each explanation is one plain sentence.
        - If a fault code is present, call the lookupDtc tool and use its result.
          Do not invent code meanings.
        - Set urgency to exactly one of STOP_NOW, DRIVE_CAREFULLY, SERVICE_SOON,
          or ROUTINE. Do not use any other label or wording.
        - Set urgency conservatively: when unsure between two levels, choose the
          more urgent one.
        - If make, model, year or key details are missing, do not assume them.
          List them in missingInfo instead.
        - Never claim certainty. You cannot inspect the car.
        - Do not give prices or write advice for the owner. Other agents do that.
        """)
    @UserMessage("Vehicle issue: {{vehicleIssue}}")
    @Agent(
            name = "Vehicle Diagnosis Agent",
            description = "Ranks likely causes and sets urgency for a described vehicle problem",
            outputKey = "diagnosis"
    )
    Diagnosis diagnose(@V("vehicleIssue") VehicleIssue vehicleIssue);
}
