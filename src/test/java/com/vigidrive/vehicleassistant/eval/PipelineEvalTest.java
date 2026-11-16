package com.vigidrive.vehicleassistant.eval;

import com.vigidrive.vehicleassistant.exception.InvalidModelOutputException;
import com.vigidrive.vehicleassistant.model.AdviceResponse;
import com.vigidrive.vehicleassistant.service.VehicleAssistantService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("eval")
@SpringBootTest
class PipelineEvalTest {

    private static final Pattern HTTP_STATUS_PATTERN = Pattern.compile("\\b(404|429|503)\\b");
    private static final List<EvalCheck> CHECKS = List.of(
            new UrgencyCheck(),
            new MustMentionCheck(),
            new MustNotContainCheck(),
            new FollowUpCheck());

    @Autowired
    private VehicleAssistantService assistantService;

    @Value("${eval.delay-ms:2000}")
    private int delayMs;

    @Value("${eval.runs-per-case:3}")
    private int runsPerCase;

    @Value("${eval.max-retries:3}")
    private int maxRetries;

    @Value("${langchain4j.open-ai.chat-model.model-name:openrouter/free}")
    private String modelName;

    @BeforeAll
    static void requireRealProviderKeyForEvalTier() {
        EvalTierConfiguration.requireApiKey(System.getenv());
    }

    @Test
    void evaluatesCasesAgainstTheConfiguredRealProvider() throws IOException {
        List<EvalCase> cases = EvalCaseLoader.load();
        List<EvalResult> results = new ArrayList<>();
        for (EvalCase evalCase : cases) {
            results.add(evaluateCase(evalCase));
        }

        EvalReportWriter.write(results, cases, modelName, gitCommitHash(), Path.of("target"));

        List<EvalResult> failedDangerousCases = results.stream()
                .filter(result -> "DANGEROUS".equalsIgnoreCase(result.group()))
                .filter(result -> result.outcome() == EvalOutcome.FAIL || !result.consistent())
                .toList();
        assertTrue(failedDangerousCases.isEmpty(),
                () -> "Dangerous evaluation cases failed or were inconsistent: " + failedDangerousCases);
    }

    private static String gitCommitHash() {
        try {
            Process process = new ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                    .redirectErrorStream(true)
                    .start();
            String hash = new String(process.getInputStream().readAllBytes()).trim();
            return process.waitFor() == 0 && !hash.isBlank() ? hash : "unavailable";
        } catch (IOException exception) {
            return "unavailable";
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "unavailable";
        }
    }

    private EvalResult evaluateCase(EvalCase evalCase) {
        List<String> urgenciesSeen = new ArrayList<>();
        List<String> failureReasons = new ArrayList<>();
        List<String> answers = new ArrayList<>();
        boolean hadInfrastructureError = false;
        boolean hadCheckFailure = false;

        for (int run = 1; run <= runsPerCase; run++) {
            if (run > 1 && !sleep(delayMs)) {
                hadInfrastructureError = true;
                failureReasons.add("run " + run + ": interrupted before invocation.");
                answers.add("ERROR: interrupted before invocation.");
                for (int skippedRun = run + 1; skippedRun <= runsPerCase; skippedRun++) {
                    failureReasons.add("run " + skippedRun + ": not run because evaluation was interrupted.");
                    answers.add("ERROR: evaluation interrupted.");
                }
                break;
            }

            AdviceResponse response;
            try {
                response = invokeWithRetries(evalCase);
            } catch (EvaluationInfrastructureException exception) {
                hadInfrastructureError = true;
                failureReasons.add("run " + run + ": ERROR " + exception.getMessage());
                answers.add("ERROR: " + exception.getMessage());
                continue;
            } catch (EvaluationOutputException exception) {
                hadInfrastructureError = true;
                failureReasons.add("run " + run + ": ERROR " + exception.getMessage());
                answers.add("ERROR: " + exception.getMessage());
                continue;
            }

            urgenciesSeen.add(response.urgency() == null ? "null" : response.urgency().name());
            answers.add(response.answer() == null ? "" : response.answer());
            for (EvalCheck check : CHECKS) {
                CheckResult checkResult = check.check(evalCase, response);
                if (!checkResult.passed()) {
                    hadCheckFailure = true;
                    failureReasons.add("run " + run + " " + checkResult.checkName()
                            + ": " + checkResult.detail());
                }
            }
        }

        boolean consistent = urgenciesSeen.stream().distinct().count() <= 1;
        EvalOutcome outcome = hadCheckFailure
                ? EvalOutcome.FAIL
                : hadInfrastructureError ? EvalOutcome.ERROR : EvalOutcome.PASS;
        return new EvalResult(evalCase.id(), evalCase.group(), answers.size(), outcome,
                urgenciesSeen, consistent, failureReasons, answers);
    }

    private AdviceResponse invokeWithRetries(EvalCase evalCase) {
        RuntimeException lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                AdviceResponse response = assistantService.ask(evalCase.input());
                if (response == null) {
                    throw new IllegalStateException("Service returned a null AdviceResponse.");
                }
                return response;
            } catch (RuntimeException exception) {
                InvalidModelOutputException invalidOutput = findInvalidModelOutput(exception);
                if (invalidOutput != null) {
                    throw new EvaluationOutputException(invalidOutput.getMessage(), invalidOutput);
                }
                lastException = exception;
                if (attempt == maxRetries) {
                    break;
                }
                long backoffMs = 1000L << attempt;
                if (!sleep(backoffMs)) {
                    throw new EvaluationInfrastructureException(
                            "interrupted during retry backoff", exception);
                }
            }
        }
        throw new EvaluationInfrastructureException(describeFailure(lastException), lastException);
    }

    private static InvalidModelOutputException findInvalidModelOutput(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof InvalidModelOutputException invalidModelOutput) {
                return invalidModelOutput;
            }
        }
        return null;
    }

    private static String describeFailure(RuntimeException exception) {
        StringBuilder messages = new StringBuilder();
        Throwable root = exception;
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            root = cause;
            if (cause.getMessage() != null) {
                messages.append(cause.getMessage()).append(' ');
            }
        }
        Matcher statusMatcher = HTTP_STATUS_PATTERN.matcher(messages);
        if (statusMatcher.find()) {
            return "HTTP " + statusMatcher.group(1);
        }
        if (root instanceof IllegalArgumentException
                && root.getMessage() != null
                && root.getMessage().startsWith("Unsupported diagnosis urgency")) {
            return root.getMessage();
        }
        return root.getClass().getSimpleName();
    }

    private static boolean sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static final class EvaluationInfrastructureException extends RuntimeException {

        private EvaluationInfrastructureException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final class EvaluationOutputException extends RuntimeException {

        private EvaluationOutputException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
