package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.VehicleIssue;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DiagnosisAgent {

    @SystemMessage("""
        You are an automotive diagnostic analyst for passenger cars in India.
        Analyze only the vehicle issue and reference data provided.
        Rules:
        - For every non-empty DTC code in the vehicle issue, call lookupDtc and
          report only the returned reference information. Never invent DTC meanings.
        - Describe the evidence and plausible diagnostic possibilities in natural
          language. Distinguish possibilities from confirmed faults.
        - Do not assign a structured urgency, recommend a repair, estimate costs,
          or give driving or safety instructions. Java performs safety checks.
        - Clearly state when the provided details or reference information are
          insufficient. Do not assume missing vehicle details.
        - Return a concise diagnostic analysis in plain text, not JSON.
        """)
    @UserMessage("Vehicle issue: {{vehicleIssue}}")
    @Agent(
            name = "Vehicle Diagnosis Agent",
            description = "Analyzes a vehicle issue and consults available DTC reference data",
            outputKey = "diagnosisAnalysis"
    )
    String diagnose(@V("vehicleIssue") VehicleIssue vehicleIssue);
}
