package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.VehicleIssue;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntakeAgentContractTest {

    @Test
    void promptExtractsOnlyStatedFactsAndForbidsDiagnosisAndSafetyDecisions() throws Exception {
        Method method = IntakeAgent.class.getMethod("intakeVehicleIssue", String.class);
        String prompt = String.join(" ",
                method.getAnnotation(SystemMessage.class).value()[0].split("\\s+"));

        assertTrue(prompt.contains("Extract only facts explicitly stated"));
        assertTrue(prompt.contains("dtcCodes"));
        assertTrue(prompt.contains("safetyConcerns"));
        assertTrue(prompt.contains("Do not diagnose"));
        assertTrue(prompt.contains("decide whether the vehicle is safe"));
        assertFalse(prompt.toLowerCase().contains("call a tool"));
        assertFalse(method.isAnnotationPresent(dev.langchain4j.agent.tool.Tool.class));
        assertTrue(method.getAnnotation(Agent.class).outputKey().equals("vehicleIssue"));
        assertTrue(method.getReturnType().equals(VehicleIssue.class));
    }
}
