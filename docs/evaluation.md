# Evaluation

[← Back to README](../README.md)

VigiDrive includes a golden dataset of 33 cases (`src/test/resources/eval-cases.json`) covering dangerous symptoms, normal faults, vague inputs, general questions and traps, such as "my brakes are fine" and unknown fault codes. Cases are in English, Hindi, Telugu and romanised text.

Each case stores expectations rather than exact answers (acceptable urgency levels, words that must or must not appear, whether a follow-up question is expected), because the answer wording changes between runs.

| Tier | What runs | LLM calls | When |
|---|---|---|---|
| Fast | `SafetyGuard` cases | none | every `mvn test` |
| Full | all cases through the whole pipeline, repeated per case | real LLM calls | on demand: `mvn test -Dgroups=eval` |

Checks are deterministic code, not an LLM judge: urgency in the acceptable set, required and forbidden words, a follow-up question for vague inputs, and urgency consistency across repeated runs. Infrastructure errors (HTTP 429, 503) are reported separately from wrong answers. The full run writes a report to `target/eval-report.md`.

<!-- TODO: fill in with your real, measured numbers after running the full tier. Delete this block if you don't have them yet. -->
**Latest results** (date: YYYY-MM-DD, model: `MODEL-NAME`, runs per case: 3)

| Group | Cases | Pass | Consistent urgency | Errors |
|---|---|---|---|---|
| Dangerous | 10 | – | – | – |
| Normal faults | 10 | – | – | – |
| Vague | 4 | – | – | – |
| General | 3 | – | – | – |
| Traps | 6 | – | – | – |

An LLM-as-judge check for answer quality is planned for CI.
