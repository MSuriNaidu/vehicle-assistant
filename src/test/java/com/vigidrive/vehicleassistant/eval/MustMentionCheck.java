package com.vigidrive.vehicleassistant.eval;

import com.vigidrive.vehicleassistant.model.AdviceResponse;

import java.util.List;
import java.util.Locale;

public final class MustMentionCheck implements EvalCheck {

    @Override
    public CheckResult check(EvalCase evalCase, AdviceResponse response) {
        String answer = response.answer() == null ? "" : response.answer().toLowerCase(Locale.ROOT);
        List<String> missing = evalCase.mustMention().stream()
                .filter(term -> !answer.contains(term.toLowerCase(Locale.ROOT)))
                .toList();
        return new CheckResult("mustMention", missing.isEmpty(),
                missing.isEmpty() ? "All required terms are present."
                        : "Missing required terms: " + missing + ".");
    }
}
