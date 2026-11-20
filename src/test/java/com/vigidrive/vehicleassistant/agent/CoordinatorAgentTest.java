package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.AdviceUrgency;
import com.vigidrive.vehicleassistant.model.DiagnosisResult;
import com.vigidrive.vehicleassistant.model.Urgency;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import com.vigidrive.vehicleassistant.safety.SafetyGuard;
import com.vigidrive.vehicleassistant.safety.SafetyService;
import com.vigidrive.vehicleassistant.service.VehicleIssueValidator;
import com.vigidrive.vehicleassistant.tools.VehicleTools;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoordinatorAgentTest {

    @Test
    void includesExtractedDtcInSharedScopeAndRoutesToDiagnosis() {
        IntakeAgent intake = mock(IntakeAgent.class);
        DiagnosisAgent diagnosis = mock(DiagnosisAgent.class);
        DiagnosisParserAgent parser = mock(DiagnosisParserAgent.class);
        AdvisorAgent advisor = mock(AdvisorAgent.class);
        VehicleTools tools = mock(VehicleTools.class);
        String message = "My 2020 Maruti Swift shows P0300 and has an engine misfire";
        VehicleIssue issue = new VehicleIssue("Maruti", "Swift", 2020,
                List.of("engine misfire"), List.of("P0300"));
        when(intake.intakeVehicleIssue(message)).thenReturn(issue);
        String analysis = "The engine misfire may be related to an ignition fault.";
        DiagnosisResult parsed = new DiagnosisResult("An ignition fault is possible.",
                Urgency.MEDIUM, List.of("Ignition fault"), List.of("Have the ignition system inspected."),
                true);
        when(diagnosis.diagnose(issue)).thenReturn(analysis);
        when(parser.parse(analysis, issue)).thenReturn(parsed);
        when(advisor.advise(message, issue, parsed, "ENGLISH", ""))
                .thenReturn("Arrange an inspection soon.");

        AdviceResponse result = coordinator(intake, diagnosis, parser, advisor, tools)
                .coordinate(message, "ENGLISH");

        assertEquals(AdviceUrgency.SERVICE_SOON, result.urgency());
        assertEquals("Arrange an inspection soon.", result.answer());
        verify(diagnosis).diagnose(issue);
        verify(parser).parse(analysis, issue);
        verify(advisor).advise(message, issue, parsed, "ENGLISH", "");
    }

    @Test
    void urgentSafetyConcernBypassesDiagnosisAndAdvisor() {
        IntakeAgent intake = mock(IntakeAgent.class);
        DiagnosisAgent diagnosis = mock(DiagnosisAgent.class);
        DiagnosisParserAgent parser = mock(DiagnosisParserAgent.class);
        AdvisorAgent advisor = mock(AdvisorAgent.class);
        VehicleTools tools = mock(VehicleTools.class);
        String message = "There is smoke coming from under the hood";
        VehicleIssue issue = new VehicleIssue(null, null, null, List.of(),
                List.of(), List.of("smoke from engine"));
        when(intake.intakeVehicleIssue(message)).thenReturn(issue);

        AdviceResponse result = coordinator(intake, diagnosis, parser, advisor, tools)
                .coordinate(message, "ENGLISH");

        assertEquals(AdviceUrgency.STOP_NOW, result.urgency());
        org.junit.jupiter.api.Assertions.assertTrue(result.safetyTriggered());
        verify(diagnosis, never()).diagnose(issue);
        verify(parser, never()).parse(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
        verify(advisor, never()).advise(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void parserValidationFailureReturnsSafeUserFacingFallback() {
        IntakeAgent intake = mock(IntakeAgent.class);
        DiagnosisAgent diagnosis = mock(DiagnosisAgent.class);
        DiagnosisParserAgent parser = mock(DiagnosisParserAgent.class);
        AdvisorAgent advisor = mock(AdvisorAgent.class);
        VehicleTools tools = mock(VehicleTools.class);
        String message = "My car is making a sound";
        VehicleIssue issue = new VehicleIssue(null, null, null, List.of("sound"), List.of());
        when(intake.intakeVehicleIssue(message)).thenReturn(issue);
        when(diagnosis.diagnose(issue)).thenReturn("Unclear diagnostic analysis.");
        when(parser.parse("Unclear diagnostic analysis.", issue))
                .thenThrow(new com.vigidrive.vehicleassistant.exception.InvalidModelOutputException(
                        "Invalid parser output"));

        AdviceResponse result = coordinator(intake, diagnosis, parser, advisor, tools)
                .coordinate(message, "ENGLISH");

        assertEquals(AdviceUrgency.SERVICE_SOON, result.urgency());
        assertEquals("I could not complete the diagnostic analysis. Please try again or provide more details.",
                result.answer());
        verify(advisor, never()).advise(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    private static CoordinatorAgent coordinator(
            IntakeAgent intake, DiagnosisAgent diagnosis, DiagnosisParserAgent parser,
            AdvisorAgent advisor, VehicleTools tools) {
        return new DefaultCoordinatorAgent(intake, diagnosis, parser, advisor,
                new SafetyService(new SafetyGuard()), new VehicleIssueValidator(), tools);
    }
}
