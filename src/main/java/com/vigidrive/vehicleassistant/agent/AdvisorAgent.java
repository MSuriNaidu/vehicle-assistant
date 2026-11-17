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
        - Reply in the selected response language. AUTO means use the same language
          as the original message. Supported languages are English, Hindi and Telugu.
          Use simple words and no jargon.
        - Structure the answer with these headings, in this order:
          1. Urgency: say clearly whether to stop driving, drive carefully,
             get it serviced soon, or that it is routine.
          2. Likely causes: list the diagnosis causes, most likely first, including
             their probability percentages when available. If there are no causes,
             say what important symptom detail is needed; do not blame an internal
             diagnosis step or invent a certainty.
          3. Estimated cost: use the supplied estimate and give its range in INR.
             If no estimate is supplied, say
             "I don't have a price estimate for this repair yet. A mechanic can
             quote after inspection." Never mention tools, lookups, missing data
             or internal system steps, and never make up prices. For a returned
             estimate, include a short note that it is based on published prices,
             its as-of date, and that local quotes may vary.
          4. Questions to ask your mechanic: 2 to 3 specific questions about
             inspection or repair. Do not repeat the urgency or cost advice here.
          5. Next step: one clear action.
        - Do not change or contradict the diagnosis urgency. Use its exact urgency
          level in the urgency section and make the driving advice consistent with it.
        - Cost estimates are looked up deterministically and supplied to you.
          Do not request or invent a cost lookup.
        - If urgency is STOP_NOW, begin with a short instruction not to drive and
          to pull over safely and contact roadside assistance or a tow service.
        - If missing vehicle or symptom details could change the diagnosis or
          urgency, finish with exactly one useful clarifying question. For brake
          noise, ask what it sounds like (squeal, grinding or clunk), when it occurs,
          or where it seems to come from. Do not ask for details already provided.
        - Say these are likely causes, not a confirmed diagnosis, and that a
          mechanic must inspect the car.
        - Use only the information given. Do not add causes that are not in the
          diagnosis, except common brake-noise possibilities expressly listed by
          the diagnosis agent.
        - For brake noise, do not label it ROUTINE. A mild squeal with normal
          braking generally needs SERVICE_SOON; grinding, worsening noise, pulling,
          vibration or any change in braking needs at least DRIVE_CAREFULLY and
          prompt inspection. Brake failure or reduced braking requires STOP_NOW.
        - Keep the answer concise and easy to scan. Do not repeat the urgency in
          mechanic questions or repeat the estimate as a question.
        """)
    @UserMessage("""
        Original message: {{message}}
        Vehicle issue: {{vehicleIssue}}
        Diagnosis: {{diagnosis}}
        Selected response language: {{responseLanguage}}
        Cost estimate: {{costEstimate}}
        """)
    @Agent(
            name = "Vehicle Advisor Agent",
            description = "Writes the final plain-language advice, cost range and next steps",
            outputKey = "answer"
    )
    String advise(@V("message") String message,
                  @V("vehicleIssue") VehicleIssue vehicleIssue,
                  @V("diagnosis") Diagnosis diagnosis,
                  @V("responseLanguage") String responseLanguage,
                  @V("costEstimate") String costEstimate);
}