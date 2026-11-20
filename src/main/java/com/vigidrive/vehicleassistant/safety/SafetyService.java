package com.vigidrive.vehicleassistant.safety;

import com.vigidrive.vehicleassistant.model.VehicleIssue;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SafetyService {

    private static final Pattern DTC_PATTERN = Pattern.compile("\\b[PCBU]\\d{4}\\b", Pattern.CASE_INSENSITIVE);

    private final SafetyGuard safetyGuard;

    public SafetyService(SafetyGuard safetyGuard) {
        this.safetyGuard = safetyGuard;
    }

    public SafetyResult evaluate(String originalMessage, VehicleIssue issue) {
        SafetyResult directRisk = safetyGuard.check(originalMessage);
        if (directRisk.dangerous()) {
            return directRisk;
        }

        if (issue != null && issue.safetyConcerns() != null) {
            for (String concern : issue.safetyConcerns()) {
                SafetyResult concernRisk = safetyGuard.check(concern);
                if (concernRisk.dangerous()) {
                    return concernRisk;
                }
            }
        }

        SafetyResult codeRisk = safetyGuard.checkDtcCodes(collectDtcCodes(originalMessage, issue));
        return codeRisk.dangerous() ? codeRisk : SafetyResult.safe();
    }

    private static List<String> collectDtcCodes(String originalMessage, VehicleIssue issue) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        if (issue != null && issue.dtcCodes() != null) {
            codes.addAll(issue.dtcCodes());
        }
        if (originalMessage != null) {
            Matcher matcher = DTC_PATTERN.matcher(originalMessage);
            while (matcher.find()) {
                codes.add(matcher.group());
            }
        }
        return new ArrayList<>(codes);
    }
}
