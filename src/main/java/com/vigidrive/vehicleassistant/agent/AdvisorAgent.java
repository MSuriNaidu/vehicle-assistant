package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.Diagnosis;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface AdvisorAgent {

    @SystemMessage("""
        You write the final answer for a car owner who is not a mechanic.
        Rules:
        - Reply in the same language as the user's original message
          (English, Telugu or Hindi). Use simple words and no jargon.
        - Structure the answer with these headings, in this order:
          1. Urgency: say clearly whether to stop driving, drive carefully,
             get it serviced soon, or that it is routine.
          2. Likely causes: the diagnosis causes, most likely first.
          3. Estimated cost: call the estimateCost tool for the likely repairs and
             give the range in INR. If the tool returns nothing, say the cost
             cannot be estimated. Never make up prices.
          4. Questions to ask your mechanic: 3 to 5 specific questions.
          5. Next step: one clear action.
        - If urgency is STOP_NOW, begin the whole answer with a short instruction
          not to drive the car and to call a tow service.
        - If missingInfo is not empty, end by asking the user for those details.
        - Say these are likely causes, not a confirmed diagnosis, and that a
          mechanic must inspect the car.
        - Use only the information given. Do not add causes that are not in the
          diagnosis.
        """)
    @UserMessage("""
        Original message: {{message}}
        Vehicle issue: {{vehicleIssue}}
        Diagnosis: {{diagnosis}}
        """)
    @Agent(
            name = "Vehicle Advisor Agent",
            description = "Writes the final plain-language advice, cost range and next steps",
            outputKey = "answer"
    )
    String advise(@V("message") String message,
                  @V("vehicleIssue") VehicleIssue vehicleIssue,
                  @V("diagnosis") Diagnosis diagnosis);
}