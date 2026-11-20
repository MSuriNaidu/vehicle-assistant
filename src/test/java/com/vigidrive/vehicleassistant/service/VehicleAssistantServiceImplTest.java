package com.vigidrive.vehicleassistant.service;

import com.vigidrive.vehicleassistant.agent.CoordinatorAgent;
import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;
import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.AdviceUrgency;
import com.vigidrive.vehicleassistant.model.ResponseLanguage;
import dev.langchain4j.service.output.OutputParsingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VehicleAssistantServiceImplTest {

    private final CoordinatorAgent coordinatorAgent = mock(CoordinatorAgent.class);
    private final VehicleAssistantServiceImpl service = new VehicleAssistantServiceImpl(coordinatorAgent);

    @Test
    void delegatesTheEntireMessageAndSelectedLanguageToTheAgenticWorkflow() {
        AdviceResponse expected = new AdviceResponse(
                AdviceUrgency.SERVICE_SOON, "Arrange an inspection.", false, null);
        when(coordinatorAgent.coordinate("My car makes a noise", "HINDI")).thenReturn(expected);

        assertEquals(expected, service.ask("My car makes a noise", ResponseLanguage.HINDI));
        verify(coordinatorAgent).coordinate("My car makes a noise", "HINDI");
    }

    @Test
    void replacesConflictingUrgencyTextWithTheStructuredUrgency() {
        AdviceResponse conflicting = new AdviceResponse(AdviceUrgency.SERVICE_SOON,
                "**Urgency:** This is routine. You can keep driving.\n\n## Likely causes\nBrake pads may be worn.",
                false, null);
        when(coordinatorAgent.coordinate("My Hyundai i20 squeaks when braking", "AUTO"))
                .thenReturn(conflicting);

        AdviceResponse answer = service.ask("My Hyundai i20 squeaks when braking");

        assertTrue(answer.answer().startsWith("**Urgency:** Arrange an inspection soon."));
        assertTrue(answer.answer().contains("## Likely causes"));
        org.junit.jupiter.api.Assertions.assertFalse(answer.answer().contains("This is routine"));
        org.junit.jupiter.api.Assertions.assertFalse(answer.answer().contains("You can keep driving"));
    }

    @Test
    void alignsHindiUrgencyTextWhenHindiIsSelected() {
        AdviceResponse conflicting = new AdviceResponse(AdviceUrgency.SERVICE_SOON,
                "**तत्कालता:** यह सामान्य समस्या है।\n\n## संभावित कारण\nब्रेक पैड घिसे हो सकते हैं।",
                false, null);
        when(coordinatorAgent.coordinate("ब्रेक से आवाज़ आ रही है", "HINDI")).thenReturn(conflicting);

        AdviceResponse answer = service.ask("ब्रेक से आवाज़ आ रही है", ResponseLanguage.HINDI);

        assertTrue(answer.answer().startsWith("**तत्कालता:** जल्द जाँच करवाएँ। समस्या बढ़े"));
        assertTrue(answer.answer().contains("## संभावित कारण"));
        org.junit.jupiter.api.Assertions.assertFalse(answer.answer().contains("यह सामान्य समस्या है"));
    }

    @Test
    void unwrapsInvalidModelOutputFromCoordinatorExceptions() {
        InvalidModelOutputException invalidOutput =
                new InvalidModelOutputException("Unsupported diagnosis urgency 'MEDIUM'.");
        when(coordinatorAgent.coordinate("My car makes a noise", "AUTO"))
                .thenThrow(new RuntimeException("Agent invocation failed", invalidOutput));

        InvalidModelOutputException thrown = assertThrows(
                InvalidModelOutputException.class, () -> service.ask("My car makes a noise"));

        assertEquals(invalidOutput.getMessage(), thrown.getMessage());
    }

    @Test
    void mapsSchemaParsingFailuresToInvalidModelOutput() {
        when(coordinatorAgent.coordinate("My car makes a noise", "AUTO"))
                .thenThrow(new RuntimeException("Agent invocation failed",
                        new OutputParsingException("Could not parse agent output", null)));

        InvalidModelOutputException thrown = assertThrows(
                InvalidModelOutputException.class, () -> service.ask("My car makes a noise"));

        assertEquals("The model returned output that does not match the required response schema.",
                thrown.getMessage());
    }
}
