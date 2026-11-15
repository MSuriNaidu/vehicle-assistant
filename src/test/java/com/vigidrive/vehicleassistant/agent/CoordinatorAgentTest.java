package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.Cause;
import com.vigidrive.vehicleassistant.model.Diagnosis;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import dev.langchain4j.agentic.AgenticServices;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoordinatorAgentTest {

    @Test
    void runsIntakeDiagnosisAndAdvisorSequentially() {
        IntakeAgent intake = mock(IntakeAgent.class);
        DiagnosisAgent diagnosis = mock(DiagnosisAgent.class);
        AdvisorAgent advisor = mock(AdvisorAgent.class);
        VehicleIssue issue = new VehicleIssue("Maruti Suzuki", "Swift", 2020,
                List.of("engine misfire"), List.of("P0300"));
        Diagnosis result = new Diagnosis("SERVICE_SOON",
                List.of(new Cause("Spark plug fault", 70, "A worn plug may cause a misfire.")));

        when(intake.intakeVehicleIssue("My 2020 Swift has a misfire")).thenReturn(issue);
        when(diagnosis.diagnose(issue)).thenReturn(result);
        when(advisor.advise("My 2020 Swift has a misfire", issue, result)).thenReturn("Have it inspected soon.");

        CoordinatorAgent coordinator = AgenticServices.sequenceBuilder(CoordinatorAgent.class)
                .subAgents(intake, diagnosis, advisor)
                .outputKey("answer")
                .build();

        assertEquals("Have it inspected soon.", coordinator.coordinate("My 2020 Swift has a misfire"));
        verify(intake).intakeVehicleIssue("My 2020 Swift has a misfire");
        verify(diagnosis).diagnose(issue);
        verify(advisor).advise("My 2020 Swift has a misfire", issue, result);
    }
}
