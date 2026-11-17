package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.Cause;
import com.vigidrive.vehicleassistant.model.Diagnosis;
import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.Urgency;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.output.JsonSchemas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoordinatorAgentTest {

    @Test
    void diagnosisSchemaRestrictsUrgencyToTheSupportedEnumValues() {
        JsonObjectSchema schema = (JsonObjectSchema) JsonSchemas.jsonSchemaFrom(Diagnosis.class)
                .orElseThrow()
                .rootElement();

        JsonEnumSchema urgencySchema = (JsonEnumSchema) schema.properties().get("urgency");
        assertEquals(List.of("STOP_NOW", "DRIVE_CAREFULLY", "SERVICE_SOON", "ROUTINE"),
                urgencySchema.enumValues());
        org.junit.jupiter.api.Assertions.assertTrue(schema.required().contains("urgency"));
    }

    @Test
    void runsDiagnosisAndAdvisorSequentiallyAndReadsTheDiagnosisFromSharedScope() {
        DiagnosisAgent diagnosis = mock(DiagnosisAgent.class);
        AdvisorAgent advisor = mock(AdvisorAgent.class);
        VehicleIssue issue = new VehicleIssue("Maruti Suzuki", "Swift", 2020,
                List.of("engine misfire"), List.of("P0300"));
        Diagnosis result = new Diagnosis(Urgency.SERVICE_SOON,
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
                            sharedDiagnosis.urgency(), answer, false, null);
                })
                .build();

        assertEquals(new AdviceResponse(Urgency.SERVICE_SOON, "Have it inspected soon?", false, null),
                coordinator.coordinate("My 2020 Swift has a misfire", issue));
        verify(diagnosis).diagnose(issue);
        verify(advisor).advise("My 2020 Swift has a misfire", issue, result);
    }
}
