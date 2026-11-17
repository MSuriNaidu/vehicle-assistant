package com.vigidrive.vehicleassistant.service;

import com.vigidrive.vehicleassistant.agent.CoordinatorAgent;
import com.vigidrive.vehicleassistant.agent.IntakeAgent;
import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;
import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.Urgency;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import com.vigidrive.vehicleassistant.safety.SafetyGuard;
import dev.langchain4j.service.output.OutputParsingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VehicleAssistantServiceImplTest {

    private final IntakeAgent intakeAgent = mock(IntakeAgent.class);
    private final CoordinatorAgent coordinatorAgent = mock(CoordinatorAgent.class);
    private final VehicleAssistantServiceImpl service =
            new VehicleAssistantServiceImpl(intakeAgent, coordinatorAgent, new SafetyGuard());

    @Test
    void returnsSafetyInstructionWithoutCallingLlmForDangerousMessage() {
        AdviceResponse answer = service.ask("The brakes failed while I was driving");

        assertEquals(Urgency.STOP_NOW, answer.urgency());
        assertTrue(answer.answer().startsWith("Stop driving."));
        assertTrue(answer.safetyTriggered());
        assertEquals("BRAKE_FAILURE", answer.safetyReason());
        verify(intakeAgent, never()).intakeVehicleIssue(anyString());
        verify(coordinatorAgent, never()).coordinate(anyString(), any(), anyString());
    }

    @Test
    void checksDtcAfterIntakeAndStopsBeforeDiagnosisAndAdvice() {
        VehicleIssue issue = new VehicleIssue(null, null, null, java.util.List.of(), java.util.List.of("P0217"));
        when(intakeAgent.intakeVehicleIssue("My scanner shows P0217")).thenReturn(issue);

        AdviceResponse answer = service.ask("My scanner shows P0217");

        assertEquals(Urgency.STOP_NOW, answer.urgency());
        assertTrue(answer.answer().startsWith("Stop driving."));
        assertTrue(answer.safetyTriggered());
        assertEquals("DTC_P0217", answer.safetyReason());
        verify(intakeAgent).intakeVehicleIssue("My scanner shows P0217");
        verify(coordinatorAgent, never()).coordinate(anyString(), any(), anyString());
    }

    @Test
    void delegatesSafeMessagesToTheSequentialCoordinator() {
        VehicleIssue issue = new VehicleIssue(null, null, null, java.util.List.of("noise"), java.util.List.of());
        AdviceResponse expected = new AdviceResponse(null, "Please describe the noise?", false, null);
        when(intakeAgent.intakeVehicleIssue("My car makes a noise")).thenReturn(issue);
        when(coordinatorAgent.coordinate("My car makes a noise", issue, "AUTO")).thenReturn(expected);

        assertEquals(expected, service.ask("My car makes a noise"));
        verify(coordinatorAgent).coordinate("My car makes a noise", issue, "AUTO");
    }

    @Test
    void forwardsTheSelectedResponseLanguageToTheCoordinator() {
        VehicleIssue issue = new VehicleIssue(null, null, null, java.util.List.of(), java.util.List.of());
        AdviceResponse expected = new AdviceResponse(null, "उत्तर", false, null);
        when(intakeAgent.intakeVehicleIssue("My car makes a noise")).thenReturn(issue);
        when(coordinatorAgent.coordinate("My car makes a noise", issue, "HINDI")).thenReturn(expected);

        assertEquals(expected, service.ask("My car makes a noise",
                com.vigidrive.vehicleassistant.model.ResponseLanguage.HINDI));
        verify(coordinatorAgent).coordinate("My car makes a noise", issue, "HINDI");
    }

    @Test
    void replacesConflictingUrgencyTextWithTheStructuredUrgency() {
        VehicleIssue issue = new VehicleIssue("Hyundai", "i20", 2020,
                java.util.List.of("brake squeal"), java.util.List.of());
        AdviceResponse conflicting = new AdviceResponse(Urgency.SERVICE_SOON,
                "**Urgency:** This is routine. You can keep driving.\n\n## Likely causes\nBrake pads may be worn.",
                false, null);
        when(intakeAgent.intakeVehicleIssue("My Hyundai i20 squeaks when braking")).thenReturn(issue);
        when(coordinatorAgent.coordinate("My Hyundai i20 squeaks when braking", issue, "AUTO"))
                .thenReturn(conflicting);

        AdviceResponse answer = service.ask("My Hyundai i20 squeaks when braking");

        assertTrue(answer.answer().startsWith(
                "**Urgency:** Arrange an inspection soon."));
        assertTrue(answer.answer().contains("## Likely causes"));
        assertFalse(answer.answer().contains("This is routine"));
        assertFalse(answer.answer().contains("You can keep driving"));
    }

    @Test
    void alignsHindiUrgencyTextWhenHindiIsSelected() {
        VehicleIssue issue = new VehicleIssue(null, null, null, java.util.List.of("brake squeal"), java.util.List.of());
        AdviceResponse conflicting = new AdviceResponse(Urgency.SERVICE_SOON,
                "**तत्कालता:** यह सामान्य समस्या है।\n\n## संभावित कारण\nब्रेक पैड घिसे हो सकते हैं।",
                false, null);
        when(intakeAgent.intakeVehicleIssue("ब्रेक से आवाज़ आ रही है")).thenReturn(issue);
        when(coordinatorAgent.coordinate("ब्रेक से आवाज़ आ रही है", issue, "HINDI")).thenReturn(conflicting);

        AdviceResponse answer = service.ask("ब्रेक से आवाज़ आ रही है",
                com.vigidrive.vehicleassistant.model.ResponseLanguage.HINDI);

        assertTrue(answer.answer().startsWith(
                "**तत्कालता:** जल्द जाँच करवाएँ। समस्या बढ़े"));
        assertTrue(answer.answer().contains("## संभावित कारण"));
        assertFalse(answer.answer().contains("यह सामान्य समस्या है"));
    }

    @Test
    void unwrapsInvalidModelOutputFromCoordinatorExceptions() {
        VehicleIssue issue = new VehicleIssue(null, null, null, java.util.List.of(), java.util.List.of());
        when(intakeAgent.intakeVehicleIssue("My car makes a noise")).thenReturn(issue);
        InvalidModelOutputException invalidOutput =
                new InvalidModelOutputException("Unsupported diagnosis urgency 'MEDIUM'.");
        when(coordinatorAgent.coordinate("My car makes a noise", issue, "AUTO"))
                .thenThrow(new RuntimeException("Agent invocation failed", invalidOutput));

        InvalidModelOutputException thrown = assertThrows(
                InvalidModelOutputException.class, () -> service.ask("My car makes a noise"));

        assertEquals(invalidOutput.getMessage(), thrown.getMessage());
    }

    @Test
    void mapsSchemaParsingFailuresToInvalidModelOutput() {
        VehicleIssue issue = new VehicleIssue(null, null, null, java.util.List.of(), java.util.List.of());
        when(intakeAgent.intakeVehicleIssue("My car makes a noise")).thenReturn(issue);
        when(coordinatorAgent.coordinate("My car makes a noise", issue, "AUTO"))
                .thenThrow(new RuntimeException("Agent invocation failed",
                        new OutputParsingException("Could not parse agent output", null)));

        InvalidModelOutputException thrown = assertThrows(
                InvalidModelOutputException.class, () -> service.ask("My car makes a noise"));

        assertEquals("The model returned output that does not match the required response schema.",
                thrown.getMessage());
    }
}
