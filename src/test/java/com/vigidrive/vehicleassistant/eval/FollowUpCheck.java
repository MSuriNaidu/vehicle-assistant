package com.vigidrive.vehicleassistant.eval;

import com.vigidrive.vehicleassistant.model.AdviceResponse;

public final class FollowUpCheck implements EvalCheck {

    @Override
    public CheckResult check(EvalCase evalCase, AdviceResponse response) {
        if (!Boolean.TRUE.equals(evalCase.expectFollowUp())) {
            return new CheckResult("followUp", true, "Skipped: follow-up question not required.");
        }
        boolean passed = response.answer() != null && response.answer().contains("?");
        return new CheckResult("followUp", passed,
                passed ? "Answer contains a follow-up question."
                        : "Expected a follow-up question mark in the answer.");
    }
}
