package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.DiagnosisResult;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DiagnosisParserAgent {

    @SystemMessage("""
        Convert the supplied diagnostic analysis into the required structured
        diagnosis result.
        Rules:
        - Return only the structured fields required by the response schema.
        - Use only information present in the diagnostic analysis and VehicleIssue.
        - Use urgency exactly as one of LOW, MEDIUM, HIGH, or CRITICAL.
        - Use LOW when urgency cannot be determined from the supplied information.
        - possibleCauses and recommendedActions must always be arrays; use empty
          arrays when the analysis does not support any.
        - Never invent DTC meanings, repair costs, or driving/safety instructions.
        - Do not turn speculation into a confirmed fault. Set
          requiresProfessionalInspection only when supported by the analysis.
        """)
    @UserMessage("""
        Vehicle issue: {{vehicleIssue}}

        Diagnostic analysis:
        {{diagnosisAnalysis}}
        """)
    @Agent(
            name = "Vehicle Diagnosis Parser Agent",
            description = "Converts tool-backed diagnostic analysis into validated structured data",
            outputKey = "diagnosisResult"
    )
    DiagnosisResult parse(@V("diagnosisAnalysis") String diagnosisAnalysis,
                          @V("vehicleIssue") VehicleIssue vehicleIssue);
}
