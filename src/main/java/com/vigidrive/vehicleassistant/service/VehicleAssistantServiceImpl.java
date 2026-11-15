package com.vigidrive.vehicleassistant.service;

import com.vigidrive.vehicleassistant.agent.CoordinatorAgent;
import com.vigidrive.vehicleassistant.safety.SafetyGuard;
import com.vigidrive.vehicleassistant.safety.SafetyResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VehicleAssistantServiceImpl implements VehicleAssistantService {

    private static final Pattern DTC_CODE_PATTERN =
            Pattern.compile("\\bP\\d{4}\\b", Pattern.CASE_INSENSITIVE);

    private final CoordinatorAgent coordinatorAgent;
    private final SafetyGuard safetyGuard;

    public VehicleAssistantServiceImpl(CoordinatorAgent coordinatorAgent, SafetyGuard safetyGuard) {
        this.coordinatorAgent = coordinatorAgent;
        this.safetyGuard = safetyGuard;
    }

    @Override
    public String ask(String question) {
        SafetyResult messageSafety = safetyGuard.check(question);
        if (messageSafety.dangerous()) {
            return messageSafety.message();
        }

        SafetyResult dtcSafety = safetyGuard.checkDtcCodes(extractDtcCodes(question));
        if (dtcSafety.dangerous()) {
            return dtcSafety.message();
        }

        return coordinatorAgent.coordinate(question);
    }

    private static List<String> extractDtcCodes(String message) {
        if (message == null || message.isBlank()) {
            return List.of();
        }
        Matcher matcher = DTC_CODE_PATTERN.matcher(message);
        List<String> codes = new ArrayList<>();
        while (matcher.find()) {
            codes.add(matcher.group());
        }
        return codes;
    }
}
