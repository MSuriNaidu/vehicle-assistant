package com.vigidrive.vehicleassistant.service;

import com.vigidrive.vehicleassistant.agent.CoordinatorAgent;
import com.vigidrive.vehicleassistant.agent.IntakeAgent;
import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;
import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.Urgency;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import com.vigidrive.vehicleassistant.safety.SafetyGuard;
import com.vigidrive.vehicleassistant.safety.SafetyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VehicleAssistantServiceImpl implements VehicleAssistantService {

    private static final Logger log = LoggerFactory.getLogger(VehicleAssistantServiceImpl.class);
    private static final Pattern DTC_CODE_PATTERN =
            Pattern.compile("\\bP\\d{4}\\b", Pattern.CASE_INSENSITIVE);

    private final IntakeAgent intakeAgent;
    private final CoordinatorAgent coordinatorAgent;
    private final SafetyGuard safetyGuard;

    public VehicleAssistantServiceImpl(IntakeAgent intakeAgent,
                                       CoordinatorAgent coordinatorAgent,
                                       SafetyGuard safetyGuard) {
        this.intakeAgent = intakeAgent;
        this.coordinatorAgent = coordinatorAgent;
        this.safetyGuard = safetyGuard;
    }

    @Override
    public AdviceResponse ask(String question) {
        String requestId = UUID.randomUUID().toString();
        String previousRequestId = MDC.get("requestId");
        MDC.put("requestId", requestId);
        long startTime = System.nanoTime();

        try {
            log.info("Vehicle assistant request started");

            SafetyResult messageSafety = safetyGuard.check(question);
            if (messageSafety.dangerous()) {
                log.warn("Request stopped by safety guard: reason={}", messageSafety.reason());
                return safetyResponse(messageSafety);
            }

            log.info("Starting intake agent");
            VehicleIssue vehicleIssue = intakeAgent.intakeVehicleIssue(question);
            List<String> dtcCodes = new ArrayList<>(extractDtcCodes(question));
            if (vehicleIssue != null && vehicleIssue.dtcCodes() != null) {
                dtcCodes.addAll(vehicleIssue.dtcCodes());
            }
            SafetyResult dtcSafety = safetyGuard.checkDtcCodes(dtcCodes);
            if (dtcSafety.dangerous()) {
                log.warn("Request stopped by DTC safety check: reason={}", dtcSafety.reason());
                return safetyResponse(dtcSafety);
            }

            log.info("Starting diagnosis and advisor sequence");
            AdviceResponse answer = coordinatorAgent.coordinate(question, vehicleIssue);
            log.info("Vehicle assistant request completed: durationMs={}", elapsedMillis(startTime));
            return answer;
        } catch (RuntimeException exception) {
            log.error("Vehicle assistant request failed: durationMs={}", elapsedMillis(startTime), exception);
            InvalidModelOutputException invalidOutput = findInvalidModelOutput(exception);
            if (invalidOutput != null) {
                throw invalidOutput;
            }
            throw exception;
        } finally {
            if (previousRequestId == null) {
                MDC.remove("requestId");
            } else {
                MDC.put("requestId", previousRequestId);
            }
        }
    }

    private static AdviceResponse safetyResponse(SafetyResult result) {
        return new AdviceResponse(Urgency.STOP_NOW, result.message(), true, result.reason());
    }

    private static long elapsedMillis(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
    }

    private static InvalidModelOutputException findInvalidModelOutput(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof InvalidModelOutputException invalidModelOutput) {
                return invalidModelOutput;
            }
        }
        return null;
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
