package com.vigidrive.vehicleassistant.eval;

import com.vigidrive.vehicleassistant.model.AdviceResponse;

/**
 * Deterministic checks for an evaluation response. A future LlmJudgeCheck may implement this
 * interface, but judge checks must remain disabled by default and require an explicit property
 * such as {@code eval.judge.enabled}.
 */
public interface EvalCheck {

    CheckResult check(EvalCase evalCase, AdviceResponse response);
}
