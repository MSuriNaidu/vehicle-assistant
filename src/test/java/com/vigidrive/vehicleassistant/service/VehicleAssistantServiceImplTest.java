package com.vigidrive.vehicleassistant.service;

import com.vigidrive.vehicleassistant.agent.CoordinatorAgent;
import com.vigidrive.vehicleassistant.agent.IntakeAgent;
import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;
import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.Urgency;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import com.vigidrive.vehicleassistant.safety.SafetyGuard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        verify(coordinatorAgent, never()).coordinate(anyString(), any());
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
        verify(coordinatorAgent, never()).coordinate(anyString(), any());
    }

    @Test
    void delegatesSafeMessagesToTheSequentialCoordinator() {
        VehicleIssue issue = new VehicleIssue(null, null, null, java.util.List.of("noise"), java.util.List.of());
        AdviceResponse expected = new AdviceResponse(null, "Please describe the noise?", false, null);
        when(intakeAgent.intakeVehicleIssue("My car makes a noise")).thenReturn(issue);
        when(coordinatorAgent.coordinate("My car makes a noise", issue)).thenReturn(expected);

        assertEquals(expected, service.ask("My car makes a noise"));
        verify(coordinatorAgent).coordinate("My car makes a noise", issue);
    }

    @Test
    void unwrapsInvalidModelOutputFromCoordinatorExceptions() {
        VehicleIssue issue = new VehicleIssue(null, null, null, java.util.List.of(), java.util.List.of());
        when(intakeAgent.intakeVehicleIssue("My car makes a noise")).thenReturn(issue);
        InvalidModelOutputException invalidOutput =
                new InvalidModelOutputException("Unsupported diagnosis urgency 'MEDIUM'.");
        when(coordinatorAgent.coordinate("My car makes a noise", issue))
                .thenThrow(new RuntimeException("Agent invocation failed", invalidOutput));

        InvalidModelOutputException thrown = assertThrows(
                InvalidModelOutputException.class, () -> service.ask("My car makes a noise"));

        assertEquals(invalidOutput.getMessage(), thrown.getMessage());
    }
}
