package com.vigidrive.vehicleassistant.service;

import com.vigidrive.vehicleassistant.agent.CoordinatorAgent;
import com.vigidrive.vehicleassistant.safety.SafetyGuard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VehicleAssistantServiceImplTest {

    private final CoordinatorAgent coordinatorAgent = mock(CoordinatorAgent.class);
    private final VehicleAssistantServiceImpl service =
            new VehicleAssistantServiceImpl(coordinatorAgent, new SafetyGuard());

    @Test
    void returnsSafetyInstructionWithoutCallingLlmForDangerousMessage() {
        String answer = service.ask("The brakes failed while I was driving");

        assertTrue(answer.startsWith("Stop driving."));
        verify(coordinatorAgent, never()).coordinate(anyString());
    }

    @Test
    void returnsSafetyInstructionForDangerousDtcWithoutCallingLlm() {
        String answer = service.ask("My scanner shows P0217");

        assertTrue(answer.startsWith("Stop driving."));
        verify(coordinatorAgent, never()).coordinate(anyString());
    }

    @Test
    void delegatesSafeMessagesToTheSequentialCoordinator() {
        when(coordinatorAgent.coordinate("My car makes a noise")).thenReturn("Please describe the noise.");

        assertEquals("Please describe the noise.", service.ask("My car makes a noise"));
        verify(coordinatorAgent).coordinate("My car makes a noise");
    }
}
