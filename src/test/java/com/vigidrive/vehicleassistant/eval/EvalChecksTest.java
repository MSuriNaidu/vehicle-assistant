package com.vigidrive.vehicleassistant.eval;

import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.model.Urgency;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalChecksTest {

    @Test
    void checksUrgencyAndRequiredTermsWithoutCaseSensitivity() {
        EvalCase evalCase = evaluationCase(
                List.of(Urgency.SERVICE_SOON), List.of("Oil", "Check engine"), List.of(), false);
        AdviceResponse response = response(Urgency.SERVICE_SOON, "Check Engine and OIL soon.");

        assertTrue(new UrgencyCheck().check(evalCase, response).passed());
        assertTrue(new MustMentionCheck().check(evalCase, response).passed());
        assertTrue(new MustNotContainCheck().check(evalCase, response).passed());
    }

    @Test
    void reportsUnexpectedUrgencyAndForbiddenTerms() {
        EvalCase evalCase = evaluationCase(
                List.of(Urgency.ROUTINE), List.of(), List.of("ignore safety"), false);
        AdviceResponse response = response(Urgency.STOP_NOW, "Please IGNORE SAFETY.");

        assertFalse(new UrgencyCheck().check(evalCase, response).passed());
        assertFalse(new MustNotContainCheck().check(evalCase, response).passed());
    }

    @Test
    void checksRequiredFollowUpQuestionAndSkipsUnrequestedFollowUp() {
        EvalCase expectsFollowUp = evaluationCase(List.of(), List.of(), List.of(), true);
        EvalCase noFollowUp = evaluationCase(List.of(), List.of(), List.of(), false);

        assertFalse(new FollowUpCheck().check(expectsFollowUp, response(null, "Describe the sound.")).passed());
        assertTrue(new FollowUpCheck().check(expectsFollowUp, response(null, "When did it start?")).passed());
        assertTrue(new FollowUpCheck().check(noFollowUp, response(null, "Okay.")).passed());
        assertTrue(new UrgencyCheck().check(noFollowUp, response(null, "Okay.")).passed());
    }

    private static EvalCase evaluationCase(
            List<Urgency> acceptableUrgency,
            List<String> mustMention,
            List<String> mustNotContain,
            boolean expectFollowUp) {
        return new EvalCase("TEST", "NORMAL", "en", "input", acceptableUrgency,
                mustMention, mustNotContain, expectFollowUp, "");
    }

    private static AdviceResponse response(Urgency urgency, String answer) {
        return new AdviceResponse(urgency, answer, false, null);
    }
}
