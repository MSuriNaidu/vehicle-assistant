package com.vigidrive.vehicleassistant.eval;

import com.vigidrive.vehicleassistant.model.Urgency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalReportWriterTest {

    @TempDir
    Path tempDirectory;

    @Test
    void rendersTotalsAndAnswersForCasesMarkedForHumanReview() {
        List<EvalCase> cases = List.of(
                evaluationCase("R01", "NORMAL", "Read the answer carefully."),
                evaluationCase("R02", "GENERAL", "check by reading the response"));
        List<EvalResult> results = List.of(
                result("R01", "NORMAL", EvalOutcome.PASS, List.of("DRIVE_CAREFULLY"),
                        List.of("Actual answer one.", "Actual answer two?")),
                result("R02", "GENERAL", EvalOutcome.ERROR, List.of(), List.of("ERROR: TimeoutException")));

        String report = EvalReportWriter.render(
                results, cases, "openrouter/free", "abc1234", Instant.parse("2026-11-16T12:00:00Z"));

        assertTrue(report.contains("**Model:** openrouter/free"));
        assertTrue(report.contains("**Git commit:** abc1234"));
        assertTrue(report.contains("| R01 | NORMAL | PASS | DRIVE_CAREFULLY | yes | - |"));
        assertTrue(report.contains("| NORMAL | 1 | 0 | 0 | 1 |"));
        assertTrue(report.contains("| Overall | 1 | 0 | 1 | 2 |"));
        assertTrue(report.contains("Actual answer one."));
        assertTrue(report.contains("Actual answer two?"));
        assertTrue(report.contains("ERROR: TimeoutException"));
    }

    @Test
    void writesBothReportsAndPrintsTheSameContents() throws IOException {
        List<EvalCase> cases = List.of(evaluationCase("R01", "NORMAL", ""));
        List<EvalResult> results = List.of(result("R01", "NORMAL", EvalOutcome.PASS,
                List.of(Urgency.ROUTINE.name()), List.of("All good.")));
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try (PrintStream captured = new PrintStream(console, true, StandardCharsets.UTF_8)) {
            System.setOut(captured);
            EvalReportWriter.write(results, cases, "model", "hash", tempDirectory);
        } finally {
            System.setOut(originalOut);
        }

        String report = Files.readString(tempDirectory.resolve("eval-report.md"));
        assertEquals(report, console.toString(StandardCharsets.UTF_8));
        try (var files = Files.list(tempDirectory)) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString()
                    .matches("eval-report-\\d{8}-\\d{6}-\\d{3}\\.md")));
        }
    }

    private static EvalCase evaluationCase(String id, String group, String notes) {
        return new EvalCase(id, group, "en", "input", List.of(), List.of(), List.of(), false, notes);
    }

    private static EvalResult result(
            String id, String group, EvalOutcome outcome, List<String> urgencies, List<String> answers) {
        return new EvalResult(id, group, answers.size(), outcome, urgencies, true, List.of(), answers);
    }
}
