package com.vigidrive.vehicleassistant.eval;

import com.vigidrive.vehicleassistant.model.AdviceResponse;

public final class UrgencyCheck implements EvalCheck {

    @Override
    public CheckResult check(EvalCase evalCase, AdviceResponse response) {
        if (evalCase.acceptableUrgency().isEmpty()) {
            return new CheckResult("urgency", true, "Skipped: no acceptable urgency specified.");
        }
        boolean passed = response.urgency() != null
                && evalCase.acceptableUrgency().contains(response.urgency());
        return new CheckResult("urgency", passed,
                passed ? "Urgency matched: " + response.urgency()
                        : "Expected one of " + evalCase.acceptableUrgency()
                                + " but received " + response.urgency() + ".");
    }
}
