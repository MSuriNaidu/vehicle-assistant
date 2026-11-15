package com.vigidrive.vehicleassistant.agent;

import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.service.V;

public interface CoordinatorAgent {

    @SequenceAgent(
            name = "Vehicle Assistant Coordinator",
            description = "Runs intake, diagnosis, and owner-advice agents in order",
            subAgents = {IntakeAgent.class, DiagnosisAgent.class, AdvisorAgent.class},
            outputKey = "answer"
    )
    String coordinate(@V("message") String message);
}
