package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.Cause;
import com.vigidrive.vehicleassistant.model.Diagnosis;
import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.Urgency;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.output.JsonSchemas;
import com.vigidrive.vehicleassistant.tools.VehicleTools;
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
    void runsDiagnosisThenLooksUpCostDeterministicallyBeforeAdvising() {
        DiagnosisAgent diagnosis = mock(DiagnosisAgent.class);
        AdvisorAgent advisor = mock(AdvisorAgent.class);
        VehicleTools vehicleTools = mock(VehicleTools.class);
        VehicleIssue issue = new VehicleIssue("Maruti Suzuki", "Swift", 2020,
                List.of("engine misfire"), List.of("P0300"));
        Diagnosis result = new Diagnosis(Urgency.SERVICE_SOON,
                List.of(new Cause("Spark plug fault", 70, "A worn plug may cause a misfire.")));

        when(diagnosis.diagnose(issue)).thenReturn(result);
        when(vehicleTools.estimateCost("Spark plug fault", "2020 Maruti Suzuki Swift"))
                .thenReturn("Indicative estimate for spark plug replacement on Swift: INR 500-900.");
        when(advisor.advise("My 2020 Swift has a misfire", issue, result, "ENGLISH",
                "Indicative estimate for spark plug replacement on Swift: INR 500-900."))
                .thenReturn("Have it inspected soon?");

        CoordinatorAgent coordinator = new DefaultCoordinatorAgent(diagnosis, advisor, vehicleTools);

        assertEquals(new AdviceResponse(Urgency.SERVICE_SOON, "Have it inspected soon?", false, null),
                coordinator.coordinate("My 2020 Swift has a misfire", issue, "ENGLISH"));
        verify(diagnosis).diagnose(issue);
        verify(vehicleTools).estimateCost("Spark plug fault", "2020 Maruti Suzuki Swift");
        verify(advisor).advise("My 2020 Swift has a misfire", issue, result, "ENGLISH",
                "Indicative estimate for spark plug replacement on Swift: INR 500-900.");
    }

    @Test
    void looksUpBrakePadPriceForLikelyPadWearWithoutAnLlmToolCall() {
        DiagnosisAgent diagnosis = mock(DiagnosisAgent.class);
        AdvisorAgent advisor = mock(AdvisorAgent.class);
        VehicleTools vehicleTools = mock(VehicleTools.class);
        VehicleIssue issue = new VehicleIssue("Maruti Suzuki", "Swift", 2019,
                List.of("brake squeal"), List.of());
        Diagnosis result = new Diagnosis(Urgency.SERVICE_SOON,
                List.of(new Cause("Worn brake pads", 60, "The pads may have reached their wear indicators.")));
        String estimate = "Indicative estimate for Brake pad replacement: INR 950-2450.";

        when(diagnosis.diagnose(issue)).thenReturn(result);
        when(vehicleTools.estimateCost("Brake pad replacement", "2019 Maruti Suzuki Swift"))
                .thenReturn(estimate);
        when(advisor.advise("My Swift squeals when braking", issue, result, "TELUGU", estimate))
                .thenReturn("తెలుగులో సమాధానం");

        AdviceResponse response = new DefaultCoordinatorAgent(diagnosis, advisor, vehicleTools)
                .coordinate("My Swift squeals when braking", issue, "TELUGU");

        assertEquals(Urgency.SERVICE_SOON, response.urgency());
        assertEquals("తెలుగులో సమాధానం", response.answer());
        verify(advisor).advise("My Swift squeals when braking", issue, result, "TELUGU", estimate);
    }
}
