package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;
import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.AdviceUrgency;
import com.vigidrive.vehicleassistant.model.DiagnosisResult;
import com.vigidrive.vehicleassistant.model.Urgency;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import com.vigidrive.vehicleassistant.safety.SafetyResult;
import com.vigidrive.vehicleassistant.safety.SafetyService;
import com.vigidrive.vehicleassistant.service.VehicleIssueValidator;
import com.vigidrive.vehicleassistant.tools.VehicleTools;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.workflow.ConditionalAgentService;
import dev.langchain4j.agentic.workflow.SequentialAgentService;
import dev.langchain4j.service.output.OutputParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultCoordinatorAgent implements CoordinatorAgent {

    private static final Logger log = LoggerFactory.getLogger(DefaultCoordinatorAgent.class);
    private static final String DIAGNOSTIC_FALLBACK =
            "I could not complete the diagnostic analysis. Please try again or provide more details.";

    private final UntypedAgent workflow;

    public DefaultCoordinatorAgent(IntakeAgent intakeAgent,
                                   DiagnosisAgent diagnosisAgent,
                                   DiagnosisParserAgent diagnosisParserAgent,
                                   AdvisorAgent advisorAgent,
                                   SafetyService safetyService,
                                   VehicleIssueValidator issueValidator,
                                   VehicleTools vehicleTools) {

        Object safetyDecision = AgenticServices.agentAction(scope -> {
            String message = (String) scope.readState("message");
            VehicleIssue issue = issueValidator.validate(
                    (VehicleIssue) scope.readState("vehicleIssue"), message);
            scope.writeState("vehicleIssue", issue);
            scope.writeState("safetyResult", safetyService.evaluate(message, issue));
        });

        Object validateDiagnosis = AgenticServices.agentAction(scope -> {
            DiagnosisResult diagnosis = (DiagnosisResult) scope.readState("diagnosisResult");
            validateDiagnosis(diagnosis);
        });

        Object estimateCost = AgenticServices.agentAction(scope -> {
            VehicleIssue issue = (VehicleIssue) scope.readState("vehicleIssue");
            DiagnosisResult diagnosis = (DiagnosisResult) scope.readState("diagnosisResult");
            scope.writeState("costEstimate", estimateLikelyRepair(diagnosis, issue, vehicleTools));
        });

        Object assembleAdvisorResponse = AgenticServices.agentAction(scope -> {
            DiagnosisResult diagnosis = (DiagnosisResult) scope.readState("diagnosisResult");
            String answer = (String) scope.readState("advisorResult");
            scope.writeState("finalAdviceResponse",
                    new AdviceResponse(toAdviceUrgency(diagnosis.urgency()), answer, false, null));
        });

        Object safeBranch = AgenticServices.sequenceBuilder()
                .subAgents(diagnosisAgent, diagnosisParserAgent, validateDiagnosis,
                        estimateCost, advisorAgent, assembleAdvisorResponse)
                .build();

        Object urgentBranch = AgenticServices.agentAction(scope -> {
            SafetyResult result = (SafetyResult) scope.readState("safetyResult");
            scope.writeState("advisorResult", result.message());
            scope.writeState("finalAdviceResponse",
                    new AdviceResponse(AdviceUrgency.STOP_NOW, result.message(), true, result.reason()));
        });

        ConditionalAgentService<UntypedAgent> branches = AgenticServices.conditionalBuilder();
        Object conditional = branches
                .subAgents(scope -> !isDangerous(scope), safeBranch)
                .subAgents(DefaultCoordinatorAgent::isDangerous, urgentBranch)
                .build();

        SequentialAgentService<UntypedAgent> sequence = AgenticServices.sequenceBuilder();
        this.workflow = sequence
                .subAgents(intakeAgent, safetyDecision, conditional)
                .output(scope -> scope.readState("advisorResult"))
                .build();
    }

    @Override
    public AdviceResponse coordinate(String message, String responseLanguage) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("message", message);
        input.put("responseLanguage", responseLanguage);

        try {
            AgenticScope scope = workflow.invokeWithAgenticScope(input).agenticScope();
            AdviceResponse response = (AdviceResponse) scope.readState("finalAdviceResponse");
            if (response == null) {
                throw new InvalidModelOutputException(
                        "The vehicle assistant workflow returned no final response.");
            }
            return response;
        } catch (RuntimeException exception) {
            if (isDiagnosisOutputFailure(exception)) {
                log.warn("Structured diagnosis parsing or validation failed; returning fallback response");
                return new AdviceResponse(AdviceUrgency.SERVICE_SOON, DIAGNOSTIC_FALLBACK, false, null);
            }
            throw exception;
        }
    }

    private static void validateDiagnosis(DiagnosisResult diagnosis) {
        if (diagnosis == null || diagnosis.summary() == null || diagnosis.summary().isBlank()
                || diagnosis.urgency() == null || diagnosis.possibleCauses() == null
                || diagnosis.recommendedActions() == null) {
            throw new InvalidModelOutputException(
                    "Structured diagnosis is missing required fields.");
        }
    }

    private static boolean isDiagnosisOutputFailure(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof InvalidModelOutputException
                    || cause instanceof OutputParsingException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDangerous(AgenticScope scope) {
        return ((SafetyResult) scope.readState("safetyResult")).dangerous();
    }

    private static AdviceUrgency toAdviceUrgency(Urgency urgency) {
        return switch (urgency) {
            case LOW -> AdviceUrgency.ROUTINE;
            case MEDIUM -> AdviceUrgency.SERVICE_SOON;
            case HIGH -> AdviceUrgency.DRIVE_CAREFULLY;
            case CRITICAL -> AdviceUrgency.STOP_NOW;
        };
    }

    private static String estimateLikelyRepair(
            DiagnosisResult diagnosis, VehicleIssue vehicleIssue, VehicleTools vehicleTools) {
        String model = vehicleName(vehicleIssue);
        if (model.isBlank()) {
            return "";
        }

        for (String repair : diagnosis.possibleCauses().stream().distinct().toList()) {
            String estimate = vehicleTools.estimateCost(repair, model);
            if (estimate != null && estimate.startsWith("Indicative estimate for ")) {
                return estimate;
            }
        }
        return "";
    }

    private static String vehicleName(VehicleIssue issue) {
        if (issue == null) {
            return "";
        }
        return Stream.of(issue.year() == null ? null : issue.year().toString(),
                        issue.make(), issue.model())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "))
                .trim();
    }

}
