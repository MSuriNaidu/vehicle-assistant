package com.vigidrive.vehicleassistant.eval;

import com.vigidrive.vehicleassistant.model.AdviceResponse;

import java.util.List;
import java.util.Locale;

public final class MustNotContainCheck implements EvalCheck {

    @Override
    public CheckResult check(EvalCase evalCase, AdviceResponse response) {
        String answer = response.answer() == null ? "" : response.answer().toLowerCase(Locale.ROOT);
        List<String> found = evalCase.mustNotContain().stream()
                .filter(term -> answer.contains(term.toLowerCase(Locale.ROOT)))
                .toList();
        return new CheckResult("mustNotContain", found.isEmpty(),
                found.isEmpty() ? "No forbidden terms are present."
                        : "Found forbidden terms: " + found + ".");
    }
}
