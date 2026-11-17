package com.vigidrive.vehicleassistant.service;

import com.vigidrive.vehicleassistant.agent.CoordinatorAgent;
import com.vigidrive.vehicleassistant.agent.IntakeAgent;
import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;
import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.ResponseLanguage;
import com.vigidrive.vehicleassistant.model.Urgency;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import com.vigidrive.vehicleassistant.safety.SafetyGuard;
import com.vigidrive.vehicleassistant.safety.SafetyResult;
import dev.langchain4j.service.output.OutputParsingException;
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
    private static final Pattern URGENCY_HEADING_PATTERN = Pattern.compile(
            "^\\s*(?:#{1,3}\\s+)?(?:\\*{1,2})?(?:urgency|तत्कालता|అత్యవసరం)(?:\\*{1,2})?\\s*:?.*$",
            Pattern.CASE_INSENSITIVE);

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
        return ask(question, ResponseLanguage.AUTO);
    }

    @Override
    public AdviceResponse ask(String question, ResponseLanguage responseLanguage) {
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
            ResponseLanguage language = responseLanguage == null ? ResponseLanguage.AUTO : responseLanguage;
            AdviceResponse answer = coordinatorAgent.coordinate(question, vehicleIssue, language.name());
            answer = alignUrgencyText(answer, question, language);
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

    private static AdviceResponse alignUrgencyText(
            AdviceResponse response, String question, ResponseLanguage language) {
        if (response == null || response.urgency() == null || response.answer() == null) {
            return response;
        }

        String[] lines = response.answer().split("\\R", -1);
        int firstContentLine = 0;
        while (firstContentLine < lines.length && lines[firstContentLine].isBlank()) {
            firstContentLine++;
        }
        if (firstContentLine == lines.length
                || !URGENCY_HEADING_PATTERN.matcher(lines[firstContentLine]).matches()) {
            return response;
        }

        int sectionEnd = firstContentLine + 1;
        while (sectionEnd < lines.length && !lines[sectionEnd].isBlank()) {
            sectionEnd++;
        }

        ResponseLanguage effectiveLanguage = resolveLanguage(question, language);
        String localizedUrgency = urgencyText(response.urgency(), effectiveLanguage);
        String urgencyHeading = switch (effectiveLanguage) {
            case HINDI -> "तत्कालता";
            case TELUGU -> "అత్యవసరం";
            case AUTO, ENGLISH -> "Urgency";
        };
        String before = String.join("\n", java.util.Arrays.copyOfRange(lines, 0, firstContentLine));
        String after = String.join("\n", java.util.Arrays.copyOfRange(
                lines, Math.min(sectionEnd + 1, lines.length), lines.length)).stripLeading();
        String normalized = (before.isBlank() ? "" : before + "\n")
                + "**" + urgencyHeading + ":** " + localizedUrgency
                + (after.isBlank() ? "" : "\n\n" + after);
        return new AdviceResponse(response.urgency(), normalized,
                response.safetyTriggered(), response.safetyReason());
    }

    private static ResponseLanguage resolveLanguage(String question, ResponseLanguage language) {
        if (language == ResponseLanguage.AUTO) {
            if (question != null && question.matches("(?s).*[\\u0C00-\\u0C7F].*")) {
                return ResponseLanguage.TELUGU;
            }
            if (question != null && question.matches("(?s).*[\\u0900-\\u097F].*")) {
                return ResponseLanguage.HINDI;
            }
            return ResponseLanguage.ENGLISH;
        }
        return language;
    }

    private static String urgencyText(Urgency urgency, ResponseLanguage language) {
        return switch (language) {
            case HINDI -> hindiUrgencyText(urgency);
            case TELUGU -> teluguUrgencyText(urgency);
            case AUTO, ENGLISH -> englishUrgencyText(urgency);
        };
    }

    private static String englishUrgencyText(Urgency urgency) {
        return switch (urgency) {
            case STOP_NOW -> "Stop driving. Pull over safely and contact roadside assistance or a tow service.";
            case DRIVE_CAREFULLY -> "Drive only if braking and steering remain normal, and arrange a prompt inspection.";
            case SERVICE_SOON -> "Arrange an inspection soon. If symptoms worsen or affect vehicle control, stop safely and seek assistance.";
            case ROUTINE -> "No immediate danger is indicated by the details shared; arrange routine service if the symptom persists.";
        };
    }

    private static String hindiUrgencyText(Urgency urgency) {
        return switch (urgency) {
            case STOP_NOW -> "गाड़ी चलाना रोकें। सुरक्षित जगह पर रुकें और रोडसाइड सहायता या टो सेवा बुलाएँ।";
            case DRIVE_CAREFULLY -> "केवल तभी सावधानी से चलाएँ जब ब्रेक और स्टीयरिंग सामान्य हों; जल्द जाँच करवाएँ।";
            case SERVICE_SOON -> "जल्द जाँच करवाएँ। समस्या बढ़े या नियंत्रण प्रभावित हो तो सुरक्षित जगह रुककर सहायता लें।";
            case ROUTINE -> "दी गई जानकारी से तत्काल खतरा नहीं दिखता। लक्षण बने रहें तो नियमित सर्विस में जाँच करवाएँ।";
        };
    }

    private static String teluguUrgencyText(Urgency urgency) {
        return switch (urgency) {
            case STOP_NOW -> "వాహనం నడపడం ఆపండి. సురక్షితంగా ఆపి రోడ్‌సైడ్ సహాయం లేదా టో సేవకు కాల్ చేయండి.";
            case DRIVE_CAREFULLY -> "బ్రేకులు, స్టీరింగ్ సాధారణంగా పనిచేస్తేనే జాగ్రత్తగా నడపండి; త్వరగా తనిఖీ చేయించండి.";
            case SERVICE_SOON -> "త్వరలో తనిఖీ చేయించండి. సమస్య పెరిగినా లేదా వాహన నియంత్రణపై ప్రభావం చూపినా సురక్షితంగా ఆపి సహాయం పొందండి.";
            case ROUTINE -> "మీరు తెలిపిన వివరాల ప్రకారం తక్షణ ప్రమాద సూచన లేదు. లక్షణం కొనసాగితే సాధారణ సర్వీసులో తనిఖీ చేయించండి.";
        };
    }

    private static long elapsedMillis(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
    }

    private static InvalidModelOutputException findInvalidModelOutput(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof InvalidModelOutputException invalidModelOutput) {
                return invalidModelOutput;
            }
            if (cause instanceof OutputParsingException) {
                return new InvalidModelOutputException(
                        "The model returned output that does not match the required response schema.", cause);
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
