package com.vigidrive.vehicleassistant.eval;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class EvalReportWriter {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneId.systemDefault());

    private EvalReportWriter() {
    }

    public static void write(
            List<EvalResult> results,
            List<EvalCase> cases,
            String modelName,
            String gitCommitHash,
            Path outputDirectory) throws IOException {
        Instant generatedAt = Instant.now();
        String report = render(results, cases, modelName, gitCommitHash, generatedAt);
        Files.createDirectories(outputDirectory);
        Files.writeString(outputDirectory.resolve("eval-report.md"), report);
        Files.writeString(outputDirectory.resolve(
                "eval-report-" + FILE_TIMESTAMP.format(generatedAt) + ".md"), report);
        System.out.print(report);
    }

    static String render(
            List<EvalResult> results,
            List<EvalCase> cases,
            String modelName,
            String gitCommitHash,
            Instant generatedAt) {
        Map<String, EvalResult> resultsByCaseId = results.stream()
                .collect(Collectors.toMap(EvalResult::caseId, result -> result));
        StringBuilder report = new StringBuilder()
                .append("# Evaluation report\n\n")
                .append("- **Date:** ").append(DATE_FORMAT.format(generatedAt)).append('\n')
                .append("- **Model:** ").append(markdownCell(modelName)).append('\n')
                .append("- **Git commit:** ").append(markdownCell(gitCommitHash)).append("\n\n")
                .append("| Case | Group | Result | Urgencies seen | Consistent | Failure reasons |\n")
                .append("|------|-------|--------|----------------|------------|-----------------|\n");

        for (EvalResult result : results) {
            report.append("| ").append(markdownCell(result.caseId()))
                    .append(" | ").append(markdownCell(result.group()))
                    .append(" | ").append(result.outcome())
                    .append(" | ").append(markdownCell(String.join(", ", result.urgenciesSeen())))
                    .append(" | ").append(result.consistent() ? "yes" : "no")
                    .append(" | ").append(markdownCell(String.join("; ", result.failureReasons())))
                    .append(" |\n");
        }

        report.append("\n## Totals\n\n")
                .append("| Group | PASS | FAIL | ERROR | Total |\n")
                .append("|-------|------|------|-------|-------|\n");
        Map<String, List<EvalResult>> byGroup = results.stream().collect(
                Collectors.groupingBy(EvalResult::group, TreeMap::new, Collectors.toList()));
        byGroup.forEach((group, groupResults) ->
                appendTotals(report, group, groupResults));
        appendTotals(report, "Overall", results);

        report.append("\n## Needs human review\n");
        List<EvalCase> reviewCases = cases.stream()
                .filter(evalCase -> containsReviewNote(evalCase.notes()))
                .sorted(Comparator.comparing(EvalCase::id))
                .toList();
        if (reviewCases.isEmpty()) {
            report.append("\nNo cases were marked for human review.\n");
        } else {
            for (EvalCase evalCase : reviewCases) {
                report.append("\n### ").append(markdownCell(evalCase.id())).append('\n');
                EvalResult result = resultsByCaseId.get(evalCase.id());
                if (result == null || result.answers().isEmpty()) {
                    report.append("\nNo answer was recorded.\n");
                    continue;
                }
                for (int index = 0; index < result.answers().size(); index++) {
                    report.append("\n**Run ").append(index + 1).append(":**\n\n");
                    appendAnswer(report, result.answers().get(index));
                    report.append('\n');
                }
            }
        }
        return report.toString();
    }

    private static void appendTotals(StringBuilder report, String group, List<EvalResult> results) {
        long pass = results.stream().filter(result -> result.outcome() == EvalOutcome.PASS).count();
        long fail = results.stream().filter(result -> result.outcome() == EvalOutcome.FAIL).count();
        long error = results.stream().filter(result -> result.outcome() == EvalOutcome.ERROR).count();
        report.append("| ").append(markdownCell(group)).append(" | ")
                .append(pass).append(" | ").append(fail).append(" | ").append(error)
                .append(" | ").append(results.size()).append(" |\n");
    }

    private static boolean containsReviewNote(String notes) {
        if (notes == null) {
            return false;
        }
        String normalized = notes.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("read the answer") || normalized.contains("check by reading");
    }

    private static String markdownCell(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }

    private static void appendAnswer(StringBuilder report, String answer) {
        String fence = answer != null && answer.contains("```") ? "````" : "```";
        report.append(fence).append('\n');
        if (answer != null) {
            report.append(answer);
            if (!answer.endsWith("\n")) {
                report.append('\n');
            }
        }
        report.append(fence).append('\n');
    }
}
