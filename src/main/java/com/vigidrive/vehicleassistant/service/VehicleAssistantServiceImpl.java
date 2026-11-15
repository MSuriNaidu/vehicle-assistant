package com.vigidrive.vehicleassistant.service;

import com.vigidrive.vehicleassistant.agent.CoordinatorAgent;
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

    private final CoordinatorAgent coordinatorAgent;
    private final SafetyGuard safetyGuard;

    public VehicleAssistantServiceImpl(CoordinatorAgent coordinatorAgent, SafetyGuard safetyGuard) {
        this.coordinatorAgent = coordinatorAgent;
        this.safetyGuard = safetyGuard;
    }

    @Override
    public String ask(String question) {
        String requestId = UUID.randomUUID().toString();
        String previousRequestId = MDC.get("requestId");
        MDC.put("requestId", requestId);
        long startTime = System.nanoTime();

        try {
            log.info("Vehicle assistant request started");

            SafetyResult messageSafety = safetyGuard.check(question);
            if (messageSafety.dangerous()) {
                log.warn("Request stopped by safety guard: reason={}", messageSafety.reason());
                return messageSafety.message();
            }

            SafetyResult dtcSafety = safetyGuard.checkDtcCodes(extractDtcCodes(question));
            if (dtcSafety.dangerous()) {
                log.warn("Request stopped by DTC safety check: reason={}", dtcSafety.reason());
                return dtcSafety.message();
            }

            log.info("Starting sequential agent coordinator");
            String answer = coordinatorAgent.coordinate(question);
            log.info("Vehicle assistant request completed: durationMs={}", elapsedMillis(startTime));
            return answer;
        } catch (RuntimeException exception) {
            log.error("Vehicle assistant request failed: durationMs={}", elapsedMillis(startTime), exception);
            throw exception;
        } finally {
            if (previousRequestId == null) {
                MDC.remove("requestId");
            } else {
                MDC.put("requestId", previousRequestId);
            }
        }
    }

    private static long elapsedMillis(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
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
