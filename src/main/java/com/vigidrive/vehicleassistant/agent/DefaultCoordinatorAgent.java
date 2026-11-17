package com.vigidrive.vehicleassistant.agent;

import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;
import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.Cause;
import com.vigidrive.vehicleassistant.model.Diagnosis;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import com.vigidrive.vehicleassistant.tools.VehicleTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultCoordinatorAgent implements CoordinatorAgent {

    private static final String BRAKE_PAD_REPAIR = "Brake pad replacement";

    private final DiagnosisAgent diagnosisAgent;
    private final AdvisorAgent advisorAgent;
    private final VehicleTools vehicleTools;

    public DefaultCoordinatorAgent(DiagnosisAgent diagnosisAgent,
                                   AdvisorAgent advisorAgent,
                                   VehicleTools vehicleTools) {
        this.diagnosisAgent = diagnosisAgent;
        this.advisorAgent = advisorAgent;
        this.vehicleTools = vehicleTools;
    }

    @Override
    public AdviceResponse coordinate(String message, VehicleIssue vehicleIssue, String responseLanguage) {
        Diagnosis diagnosis = diagnosisAgent.diagnose(vehicleIssue);
        if (diagnosis == null || diagnosis.urgency() == null) {
            throw new InvalidModelOutputException(
                    "Diagnosis output must include a supported urgency value.");
        }

        String costEstimate = estimateLikelyRepair(diagnosis, vehicleIssue);
        String answer = advisorAgent.advise(
                message, vehicleIssue, diagnosis, responseLanguage, costEstimate);
        return new AdviceResponse(diagnosis.urgency(), answer, false, null);
    }

    private String estimateLikelyRepair(Diagnosis diagnosis, VehicleIssue vehicleIssue) {
        String model = vehicleName(vehicleIssue);
        if (model.isBlank()) {
            return "";
        }

        List<String> repairs = new ArrayList<>();
        if (diagnosis.causes() != null) {
            boolean brakePadLikely = diagnosis.causes().stream()
                    .filter(cause -> cause != null)
                    .anyMatch(DefaultCoordinatorAgent::mentionsBrakePads);
            if (brakePadLikely) {
                repairs.add(BRAKE_PAD_REPAIR);
            }
            diagnosis.causes().stream()
                    .filter(cause -> cause != null && cause.name() != null && !cause.name().isBlank())
                    .map(Cause::name)
                    .forEach(repairs::add);
        }

        for (String repair : repairs.stream().distinct().toList()) {
            String estimate = vehicleTools.estimateCost(repair, model);
            if (estimate.startsWith("Indicative estimate for ")) {
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

    private static boolean mentionsBrakePads(Cause cause) {
        return Stream.of(cause.name(), cause.explanation())
                .filter(value -> value != null)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains("pad") || value.contains("lining")
                        || value.contains("ప్యాడ్") || value.contains("ब्रेक पैड"));
    }
}
