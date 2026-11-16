package com.vigidrive.vehicleassistant.eval;

import com.vigidrive.vehicleassistant.model.Urgency;
import com.vigidrive.vehicleassistant.safety.SafetyGuard;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafetyGuardEvalTest {

    private static final Pattern DTC_PATTERN = Pattern.compile("P[0-9]{4}");

    private final SafetyGuard safetyGuard = new SafetyGuard();

    @ParameterizedTest(name = "{0}")
    @MethodSource("stopNowCases")
    void stopNowCasesAreDetectedBySafetyGuard(String caseId, EvalCase evalCase) {
        if (caseId.equals("D10")) {
            assertTrue(safetyGuard.checkDtcCodes(extractDtcCodes(evalCase.input())).dangerous(),
                    "Expected a dangerous DTC in " + caseId);
        } else {
            assertTrue(safetyGuard.check(evalCase.input()).dangerous(),
                    "Expected text safety guard to trigger for " + caseId);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("trapAndNegativeCases")
    void negativeAndPromptInjectionCasesFollowExpectedRules(String caseId, EvalCase evalCase) {
        boolean dangerous = safetyGuard.check(evalCase.input()).dangerous();
        if (caseId.equals("T06")) {
            assertTrue(dangerous, "Prompt-injection case must still trigger the safety guard.");
        } else {
            assertFalse(dangerous, "Negative case must not trigger the safety guard.");
        }
    }

    private static Stream<Arguments> stopNowCases() {
        return EvalCaseLoader.load().stream()
                .filter(evalCase -> evalCase.acceptableUrgency().equals(List.of(Urgency.STOP_NOW)))
                .map(evalCase -> Arguments.of(evalCase.id(), evalCase));
    }

    private static Stream<Arguments> trapAndNegativeCases() {
        List<String> ids = List.of("T01", "T02", "N06", "T06");
        return EvalCaseLoader.load().stream()
                .filter(evalCase -> ids.contains(evalCase.id()))
                .map(evalCase -> Arguments.of(evalCase.id(), evalCase));
    }

    private static List<String> extractDtcCodes(String input) {
        Matcher matcher = DTC_PATTERN.matcher(input);
        List<String> codes = new ArrayList<>();
        while (matcher.find()) {
            codes.add(matcher.group());
        }
        return codes;
    }
}
