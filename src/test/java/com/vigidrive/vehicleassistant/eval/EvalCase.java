package com.vigidrive.vehicleassistant.eval;

import com.vigidrive.vehicleassistant.model.Urgency;

import java.util.List;

public record EvalCase(
        String id,
        String group,
        String language,
        String input,
        List<Urgency> acceptableUrgency,
        List<String> mustMention,
        List<String> mustNotContain,
        Boolean expectFollowUp,
        String notes) {

    public EvalCase {
        acceptableUrgency = acceptableUrgency == null ? List.of() : List.copyOf(acceptableUrgency);
        mustMention = mustMention == null ? List.of() : List.copyOf(mustMention);
        mustNotContain = mustNotContain == null ? List.of() : List.copyOf(mustNotContain);
        expectFollowUp = expectFollowUp == null ? Boolean.FALSE : expectFollowUp;
    }
}
