package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.model.DiagnosisResult;
import com.vigidrive.vehicleassistant.model.Urgency;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.output.JsonSchemas;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosisParserAgentTest {

    @Test
    void parserReturnsStructuredDiagnosisAndUsesOnlyTheStructuredOutputContract() throws Exception {
        Method method = DiagnosisParserAgent.class.getMethod(
                "parse", String.class, com.vigidrive.vehicleassistant.model.VehicleIssue.class);
        String prompt = String.join(" ",
                method.getAnnotation(SystemMessage.class).value()[0].split("\\s+"));

        assertEquals(DiagnosisResult.class, method.getReturnType());
        assertEquals("diagnosisResult", method.getAnnotation(Agent.class).outputKey());
        assertTrue(prompt.contains("LOW, MEDIUM, HIGH, or CRITICAL"));
        assertTrue(prompt.contains("Never invent DTC meanings, repair costs"));
        assertFalse(method.isAnnotationPresent(dev.langchain4j.agent.tool.Tool.class));

        JsonObjectSchema schema = (JsonObjectSchema) JsonSchemas.jsonSchemaFrom(DiagnosisResult.class)
                .orElseThrow().rootElement();
        JsonEnumSchema urgency = (JsonEnumSchema) schema.properties().get("urgency");
        assertEquals(List.of("LOW", "MEDIUM", "HIGH", "CRITICAL"), urgency.enumValues());
        assertTrue(schema.required().containsAll(List.of("summary", "urgency", "possibleCauses",
                "recommendedActions", "requiresProfessionalInspection")));
    }

    @Test
    void urgencyEnumContainsOnlyTheRequestedJsonValues() {
        assertEquals(List.of("LOW", "MEDIUM", "HIGH", "CRITICAL"),
                java.util.Arrays.stream(Urgency.values()).map(Enum::name).toList());
    }
}
