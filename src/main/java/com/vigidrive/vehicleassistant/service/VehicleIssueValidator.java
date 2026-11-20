package com.vigidrive.vehicleassistant.service;

import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;
import com.vigidrive.vehicleassistant.model.VehicleIssue;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class VehicleIssueValidator {

    private static final Pattern DTC_PATTERN = Pattern.compile("(?i)[PCBU]\\d{4}");
    private static final int MAX_FIELD_LENGTH = 160;
    private static final int MAX_LIST_ITEMS = 8;

    public VehicleIssue validate(VehicleIssue issue, String originalMessage) {
        if (issue == null) {
            throw new InvalidModelOutputException("Vehicle intake returned no issue details.");
        }

        String source = originalMessage == null ? "" : originalMessage;
        String normalizedSource = source.toLowerCase(Locale.ROOT);
        String make = statedValue(issue.make(), normalizedSource);
        String model = statedValue(issue.model(), normalizedSource);
        Integer year = statedYear(issue.year(), source);
        List<String> symptoms = validatePhrases(issue.symptoms());
        List<String> safetyConcerns = validatePhrases(issue.safetyConcerns());
        List<String> dtcCodes = issue.dtcCodes().stream()
                .filter(code -> DTC_PATTERN.matcher(code).matches())
                .filter(source::contains)
                .limit(MAX_LIST_ITEMS)
                .toList();

        return new VehicleIssue(make, model, year, symptoms, dtcCodes, safetyConcerns);
    }

    private static String statedValue(String value, String source) {
        if (value == null || value.isBlank() || value.length() > MAX_FIELD_LENGTH
                || !source.contains(value.trim().toLowerCase(Locale.ROOT))) {
            return null;
        }
        return value.trim();
    }

    private static Integer statedYear(Integer year, String source) {
        if (year == null || year < 1886 || year > Year.now().getValue() + 1
                || !source.contains(year.toString())) {
            return null;
        }
        return year;
    }

    private static List<String> validatePhrases(List<String> values) {
        return values.stream()
                .filter(value -> !value.isBlank() && value.length() <= MAX_FIELD_LENGTH)
                .distinct()
                .limit(MAX_LIST_ITEMS)
                .toList();
    }
}
