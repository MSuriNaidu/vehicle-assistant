package com.vigidrive.vehicleassistant.eval;

import java.util.List;

public record EvalResult(
        String caseId,
        String group,
        int runs,
        EvalOutcome outcome,
        List<String> urgenciesSeen,
        boolean consistent,
        List<String> failureReasons,
        List<String> answers) {

    public EvalResult {
        urgenciesSeen = List.copyOf(urgenciesSeen);
        failureReasons = List.copyOf(failureReasons);
        answers = List.copyOf(answers);
    }
}
