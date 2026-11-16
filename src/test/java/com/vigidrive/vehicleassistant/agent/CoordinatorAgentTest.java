package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.Cause;
import com.vigidrive.vehicleassistant.model.Diagnosis;
import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.Urgency;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;
import dev.langchain4j.agentic.AgenticServices;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoordinatorAgentTest {

    @Test
    void rejectsUnsupportedModelUrgencyInsteadOfSilentlyMappingIt() {
        InvalidModelOutputException exception = assertThrows(
                InvalidModelOutputException.class, () -> Urgency.fromModelValue("MEDIUM"));

        assertEquals(
                "Unsupported diagnosis urgency 'MEDIUM'. Expected one of: "
                        + "STOP_NOW, DRIVE_CAREFULLY, SERVICE_SOON, ROUTINE.",
                exception.getMessage());
    }

    @Test
    void acceptsSupportedUrgencyFormattingAndExplainsUnknownValues() {
        assertEquals(Urgency.STOP_NOW, Urgency.fromModelValue("stop-now"));
        assertEquals(Urgency.SERVICE_SOON, Urgency.fromModelValue("service soon"));
        assertNull(Urgency.fromModelValue("  "));

        InvalidModelOutputException exception = assertThrows(
                InvalidModelOutputException.class, () -> Urgency.fromModelValue("UNRECOGNIZED"));
        org.junit.jupiter.api.Assertions.assertTrue(
                exception.getMessage().contains("Unsupported diagnosis urgency"));
    }

    @Test
    void runsDiagnosisAndAdvisorSequentiallyAndReadsTheDiagnosisFromSharedScope() {
        DiagnosisAgent diagnosis = mock(DiagnosisAgent.class);
        AdvisorAgent advisor = mock(AdvisorAgent.class);
        VehicleIssue issue = new VehicleIssue("Maruti Suzuki", "Swift", 2020,
                List.of("engine misfire"), List.of("P0300"));
        Diagnosis result = new Diagnosis("SERVICE_SOON",
                List.of(new Cause("Spark plug fault", 70, "A worn plug may cause a misfire.")));

        when(diagnosis.diagnose(issue)).thenReturn(result);
        when(advisor.advise("My 2020 Swift has a misfire", issue, result)).thenReturn("Have it inspected soon?");

        CoordinatorAgent coordinator = AgenticServices.sequenceBuilder(CoordinatorAgent.class)
                .subAgents(diagnosis, advisor)
                .outputKey("adviceResponse")
                .output(scope -> {
                    Diagnosis sharedDiagnosis = (Diagnosis) scope.readState("diagnosis");
                    String answer = (String) scope.readState("answer");
                    return new AdviceResponse(
                            Urgency.valueOf(sharedDiagnosis.urgency()), answer, false, null);
                })
                .build();

        assertEquals(new AdviceResponse(Urgency.SERVICE_SOON, "Have it inspected soon?", false, null),
                coordinator.coordinate("My 2020 Swift has a misfire", issue));
        verify(diagnosis).diagnose(issue);
        verify(advisor).advise("My 2020 Swift has a misfire", issue, result);
    }
}
