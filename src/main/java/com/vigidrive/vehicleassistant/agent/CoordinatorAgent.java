package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.V;

public interface CoordinatorAgent {

    @SequenceAgent(
            name = "Vehicle Assistant Coordinator",
            description = "Runs diagnosis and owner-advice agents in order after intake and safety checks",
            subAgents = {DiagnosisAgent.class, AdvisorAgent.class},
            outputKey = "adviceResponse"
    )
    AdviceResponse coordinate(@V("message") String message,
                              @V("vehicleIssue") VehicleIssue vehicleIssue);
}
